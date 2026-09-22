package com.ejyqyl.atomduels.gui;

import com.ejyqyl.atomduels.AtomDuels;
import com.ejyqyl.atomduels.data.PlayerData;
import com.ejyqyl.atomduels.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Overflow item claim menu (/duels claim).
 * Allows players to safely recover items that couldn't fit in their inventory.
 * Authored by ejyqyl.
 */
public class ClaimMenu implements AtomMenu {

    private final AtomDuels plugin;
    private final Player player;
    private final Inventory inventory;

    public ClaimMenu(AtomDuels plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.parse("<gradient:#00B5FD:#7670E5>Хранилище возврата предметов</gradient>"));
        build();
    }

    private void build() {
        PlayerData data = plugin.getStatsManager().getPlayerData(player);
        List<ItemStack> items = data.getClaimItems();

        if (items.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "claim.empty");
        }

        for (int i = 0; i < items.size() && i < 54; i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        // Player can pick up items from this menu
        if (event.getRawSlot() >= 0 && event.getRawSlot() < 54) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && !clicked.getType().isAir()) {
                PlayerData data = plugin.getStatsManager().getPlayerData(player);
                data.getClaimItems().remove(clicked);
                plugin.getStatsManager().savePlayerData(player.getUniqueId());
            }
        }
    }
}
