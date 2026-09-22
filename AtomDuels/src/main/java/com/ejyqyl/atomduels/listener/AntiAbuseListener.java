package com.ejyqyl.atomduels.listener;

import com.ejyqyl.atomduels.config.ConfigManager;
import com.ejyqyl.atomduels.config.MessageManager;
import com.ejyqyl.atomduels.duel.ActiveDuel;
import com.ejyqyl.atomduels.duel.DuelManager;
import com.ejyqyl.atomduels.duel.DuelState;
import com.ejyqyl.atomduels.rules.Rule;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * Prevents cheating, unwanted command execution, item smuggling, and enforces hunger/potion rules.
 * Authored by ejyqyl.
 */
public class AntiAbuseListener implements Listener {

    private final DuelManager duelManager;
    private final ConfigManager configManager;
    private final MessageManager messageManager;

    public AntiAbuseListener(DuelManager duelManager, ConfigManager configManager, MessageManager messageManager) {
        this.duelManager = duelManager;
        this.configManager = configManager;
        this.messageManager = messageManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (duelManager.isInDuel(player.getUniqueId())) {
            String message = event.getMessage();
            if (!configManager.isCommandAllowed(message)) {
                event.setCancelled(true);
                messageManager.sendMessage(player, "match.command-blocked");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (duelManager.isInDuel(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (duelManager.isInDuel(player.getUniqueId())) {
            InventoryType type = event.getInventory().getType();
            if (type == InventoryType.ENDER_CHEST || type == InventoryType.WORKBENCH ||
                type == InventoryType.ANVIL || type == InventoryType.ENCHANTING ||
                type == InventoryType.BEACON || type == InventoryType.SHULKER_BOX) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ActiveDuel duel = duelManager.getDuel(player.getUniqueId());
        if (duel != null) {
            if (!duel.getRuleSet().isRuleEnabled(Rule.HUNGER)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHealthRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ActiveDuel duel = duelManager.getDuel(player.getUniqueId());
        if (duel != null) {
            if (!duel.getRuleSet().isRuleEnabled(Rule.NATURAL_REGEN)) {
                if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED ||
                    event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) {
            ActiveDuel duel = duelManager.getDuel(player.getUniqueId());
            if (duel != null) {
                if (duel.getState() != DuelState.IN_FIGHT) {
                    event.setCancelled(true);
                    return;
                }
                if (event.getEntity() instanceof EnderPearl && !duel.getRuleSet().isRuleEnabled(Rule.ENDER_PEARL)) {
                    event.setCancelled(true);
                    return;
                }
                if (event.getEntity() instanceof ThrownPotion && !duel.getRuleSet().isRuleEnabled(Rule.SPLASH_POTIONS)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ActiveDuel duel = duelManager.getDuel(player.getUniqueId());
        if (duel != null) {
            Material mat = event.getItem().getType();
            if (mat == Material.POTION && !duel.getRuleSet().isRuleEnabled(Rule.POTIONS)) {
                event.setCancelled(true);
                return;
            }
            if ((mat == Material.GOLDEN_APPLE || mat == Material.ENCHANTED_GOLDEN_APPLE) &&
                !duel.getRuleSet().isRuleEnabled(Rule.GOLDEN_APPLE)) {
                event.setCancelled(true);
            }
        }
    }
}
