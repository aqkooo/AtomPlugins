package com.ejyqyl.atombot.listener;

import com.ejyqyl.atombot.entity.TrainingBot;
import com.ejyqyl.atombot.gui.BotMenu;
import com.ejyqyl.atombot.manager.BotManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Dedicated combat and damage reaction listener for AtomBot.
 * Guarantees player hit registration, shield defense, blast reduction, and immortal combo resets.
 *
 * @author ejyqyl
 */
public class BotCombatListener implements Listener {

    private final BotManager botManager;

    public BotCombatListener(BotManager botManager) {
        this.botManager = botManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity living) {
            TrainingBot bot = botManager.getBotByEntity(living);
            if (bot != null) {
                if (living.getHealth() - event.getFinalDamage() <= 0) {
                    event.setCancelled(true);
                    bot.playDamageReaction(event.getFinalDamage());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity attacker = event.getDamager();

        // 1. Victim is Bot
        TrainingBot botVictim = botManager.getBotByEntity(victim);
        if (botVictim != null) {
            // Shield blocking
            if (botVictim.getSettings().isUseShield()) {
                if (Math.random() < 0.65) {
                    event.setCancelled(true);
                    botVictim.playShieldBlock();
                    return;
                }
            }

            // Blast resistance
            if (botVictim.getSettings().isBlastResistance()) {
                if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                    event.setDamage(event.getDamage() * 0.1);
                }
            }

            botVictim.playDamageReaction(event.getFinalDamage());
            return;
        }

        // 2. Attacker is Bot
        TrainingBot botAttacker = botManager.getBotByEntity(attacker);
        if (botAttacker != null) {
            if (!botAttacker.getSettings().isAttackPlayer()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BotMenu menu) {
            menu.handleClick(event);
        }
    }
}
