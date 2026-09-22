package com.ejyqyl.atompvpbot.listener;

import com.ejyqyl.atompvpbot.entity.PvPBotEntity;
import com.ejyqyl.atompvpbot.fight.BotFight;
import com.ejyqyl.atompvpbot.fight.FightManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles damage calculation, shield blocking, infinite totem triggers, and death suppression.
 *
 * @author ejyqyl
 */
public class BotCombatListener implements Listener {

    private final FightManager fightManager;

    public BotCombatListener(FightManager fightManager) {
        this.fightManager = fightManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity damager = event.getDamager();

        // 1. Player attacks Bot
        if (victim instanceof Husk husk && damager instanceof Player player) {
            BotFight fight = fightManager.getFightByBot(husk);
            if (fight != null) {
                PvPBotEntity bot = fight.getBot();

                // Shield blocking check
                if (bot.getDefenseSystem().isBlocking()) {
                    ItemStack handItem = player.getInventory().getItemInMainHand();
                    if (handItem.getType().name().endsWith("_AXE")) {
                        // Axe breaks bot shield
                        bot.getDefenseSystem().breakShield();
                    } else {
                        // Shield fully absorbs/blocks attack
                        event.setCancelled(true);
                        husk.getWorld().playSound(husk.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                        return;
                    }
                }

                // Check lethal damage to bot
                if (husk.getHealth() - event.getFinalDamage() <= 0.0) {
                    event.setCancelled(true);
                    bot.getDefenseSystem().triggerTotem();
                    fight.onBotDefeated();
                    return;
                }
            }
        }

        // 2. Bot attacks Player
        if (victim instanceof Player player && damager instanceof Husk husk) {
            BotFight fight = fightManager.getFight(player);
            if (fight != null && fight.getState() == BotFight.State.FIGHTING) {
                // Check lethal damage to player
                if (player.getHealth() - event.getFinalDamage() <= 0.0) {
                    // Check if player has totem in hand
                    ItemStack off = player.getInventory().getItemInOffHand();
                    ItemStack main = player.getInventory().getItemInMainHand();
                    if (off.getType() != Material.TOTEM_OF_UNDYING && main.getType() != Material.TOTEM_OF_UNDYING) {
                        event.setCancelled(true);
                        fight.onPlayerDefeated();
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Husk husk) {
            BotFight fight = fightManager.getFightByBot(husk);
            if (fight != null) {
                // Prevent void or environmental lethal damage from destroying bot entity
                if (husk.getHealth() - event.getFinalDamage() <= 0.0) {
                    event.setCancelled(true);
                    fight.getBot().getDefenseSystem().triggerTotem();
                    fight.onBotDefeated();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onResurrect(EntityResurrectEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Husk husk) {
            BotFight fight = fightManager.getFightByBot(husk);
            if (fight != null) {
                event.setCancelled(false);
                fight.getBot().getDefenseSystem().triggerTotem();
                fight.onBotDefeated();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Husk husk) {
            BotFight fight = fightManager.getFightByBot(husk);
            if (fight != null) {
                event.getDrops().clear();
                event.setDroppedExp(0);
                fight.onBotDefeated();
            }
        }
    }
}
