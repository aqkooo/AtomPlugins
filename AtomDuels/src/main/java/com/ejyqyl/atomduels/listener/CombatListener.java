package com.ejyqyl.atomduels.listener;

import com.ejyqyl.atomduels.bot.BotManager;
import com.ejyqyl.atomduels.bot.DuelBot;
import com.ejyqyl.atomduels.duel.ActiveDuel;
import com.ejyqyl.atomduels.duel.DuelManager;
import com.ejyqyl.atomduels.duel.DuelState;
import com.ejyqyl.atomduels.rules.Rule;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles PvP combat, weapon rules, and damage reactions for players and bots.
 * Authored by ejyqyl (https://github.com/aqkooo).
 */
public class CombatListener implements Listener {

    private final DuelManager duelManager;
    private final BotManager botManager;

    public CombatListener(DuelManager duelManager, BotManager botManager) {
        this.duelManager = duelManager;
        this.botManager = botManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        // 1. Check if victim is a bot taking fatal damage
        if (entity instanceof LivingEntity living) {
            DuelBot bot = botManager.getBotByEntity(living);
            if (bot != null) {
                if (living.getHealth() - event.getFinalDamage() <= 0) {
                    if (bot.handleFatalDamage()) {
                        event.setCancelled(true);
                    } else if (!bot.isTrainingBot()) {
                        event.setCancelled(true);
                        duelManager.handleBotDeath(bot);
                    }
                }
                return;
            }
        }

        // 2. Check if victim is a Player in a duel
        if (!(entity instanceof Player player)) {
            return;
        }

        ActiveDuel duel = duelManager.getDuel(player.getUniqueId());
        if (duel == null) {
            return;
        }

        if (duel.getState() != DuelState.IN_FIGHT) {
            event.setCancelled(true);
            return;
        }

        // Fatal damage handling without player death screen flicker
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            if (duel.getRuleSet().isRuleEnabled(Rule.TOTEM) && hasTotem(player)) {
                return; // let vanilla totem trigger
            }

            event.setCancelled(true);
            player.setHealth(20.0);
            duelManager.handleDeath(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity attacker = event.getDamager();

        Player victimPlayer = victim instanceof Player ? (Player) victim : null;
        Player attackerPlayer = null;

        if (attacker instanceof Player) {
            attackerPlayer = (Player) attacker;
        } else if (attacker instanceof Arrow arrow && arrow.getShooter() instanceof Player shooter) {
            attackerPlayer = shooter;
        } else if (attacker instanceof Trident trident && trident.getShooter() instanceof Player shooter) {
            attackerPlayer = shooter;
        }

        // 1. Victim is a BOT!
        DuelBot botVictim = botManager.getBotByEntity(victim);
        if (botVictim != null) {
            if (attackerPlayer == null) {
                event.setCancelled(true);
                return;
            }

            if (botVictim.isTrainingBot()) {
                // Training NPC bot: check shield
                if (botVictim.getSettings() != null && botVictim.getSettings().isUseShield()) {
                    if (Math.random() < 0.60) {
                        event.setCancelled(true);
                        botVictim.playShieldBlock();
                        return;
                    }
                }
                // Check blast resistance
                if (botVictim.getSettings() != null && botVictim.getSettings().isBlastResistance()) {
                    if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                        event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                        event.setDamage(event.getDamage() * 0.1);
                    }
                }
                // Allow damage!
                botVictim.playDamageReaction(event.getFinalDamage());
                return;
            } else {
                // Duel match bot
                ActiveDuel duel = duelManager.getDuel(attackerPlayer.getUniqueId());
                if (duel == null || duel.getBot() == null || !duel.getBot().getBotId().equals(botVictim.getBotId())) {
                    event.setCancelled(true);
                    return;
                }
                if (duel.getState() != DuelState.IN_FIGHT) {
                    event.setCancelled(true);
                    return;
                }
                checkWeaponRules(event, attackerPlayer, duel);
                botVictim.playDamageReaction(event.getFinalDamage());
                return;
            }
        }

        // 2. Attacker is a BOT!
        DuelBot botAttacker = botManager.getBotByEntity(attacker);
        if (botAttacker != null) {
            if (victimPlayer == null) {
                event.setCancelled(true);
                return;
            }

            if (botAttacker.isTrainingBot()) {
                if (botAttacker.getSettings() != null && !botAttacker.getSettings().isAttackPlayer()) {
                    event.setCancelled(true);
                }
                return;
            } else {
                ActiveDuel duel = duelManager.getDuel(victimPlayer.getUniqueId());
                if (duel == null || duel.getState() != DuelState.IN_FIGHT) {
                    event.setCancelled(true);
                }
                return;
            }
        }

        // 3. Player vs Player damage
        boolean victimInDuel = victimPlayer != null && duelManager.isInDuel(victimPlayer.getUniqueId());
        boolean attackerInDuel = attackerPlayer != null && duelManager.isInDuel(attackerPlayer.getUniqueId());

        if (victimInDuel != attackerInDuel) {
            event.setCancelled(true);
            return;
        }

        if (victimInDuel && attackerInDuel) {
            ActiveDuel duel = duelManager.getDuel(victimPlayer.getUniqueId());
            if (duel == null || !duel.isParticipant(attackerPlayer.getUniqueId())) {
                event.setCancelled(true);
                return;
            }

            if (duel.getState() != DuelState.IN_FIGHT) {
                event.setCancelled(true);
                return;
            }

            checkWeaponRules(event, attackerPlayer, duel);
        }
    }

    private void checkWeaponRules(EntityDamageByEntityEvent event, Player attacker, ActiveDuel duel) {
        ItemStack item = attacker.getInventory().getItemInMainHand();
        Material mat = item.getType();

        if (event.getDamager() instanceof Arrow) {
            if (!duel.getRuleSet().isRuleEnabled(Rule.BOW) && !duel.getRuleSet().isRuleEnabled(Rule.CROSSBOW)) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getDamager() instanceof Trident && !duel.getRuleSet().isRuleEnabled(Rule.TRIDENT)) {
            event.setCancelled(true);
            return;
        }

        if (mat.name().endsWith("_SWORD") && !duel.getRuleSet().isRuleEnabled(Rule.SWORD)) {
            event.setCancelled(true);
            return;
        }

        if (mat.name().endsWith("_AXE") && !duel.getRuleSet().isRuleEnabled(Rule.AXE)) {
            event.setCancelled(true);
            return;
        }

        if (mat == Material.MACE && !duel.getRuleSet().isRuleEnabled(Rule.MACE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTotemResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player player) {
            ActiveDuel duel = duelManager.getDuel(player.getUniqueId());
            if (duel != null && !duel.getRuleSet().isRuleEnabled(Rule.TOTEM)) {
                event.setCancelled(true);
            }
        }
    }

    private boolean hasTotem(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING ||
               player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;
    }
}
