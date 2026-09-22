package com.ejyqyl.atomduels.gui;

import com.ejyqyl.atomduels.AtomDuels;
import com.ejyqyl.atomduels.data.PlayerData;
import com.ejyqyl.atomduels.util.ColorUtil;
import com.ejyqyl.atomduels.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Loot menu for collecting or returning trophies after Own Items duels (/duels loot).
 * Authored by ejyqyl.
 */
public class LootMenu implements AtomMenu {

    private final AtomDuels plugin;
    private final Player player;
    private final Inventory inventory;

    public LootMenu(AtomDuels plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.parse("<gradient:#FFD700:#FF5555>Трофеи и Лут с дуэлей</gradient>"));
        build();
    }

    private void build() {
        PlayerData data = plugin.getStatsManager().getPlayerData(player);
        List<ItemStack> loot = data.getLoserLootItems();

        for (int i = 0; i < loot.size() && i < 45; i++) {
            inventory.setItem(i, loot.get(i));
        }

        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Return button
        inventory.setItem(49, new ItemBuilder(Material.EMERALD)
                .name("<#00FF88>Вернуть все предметы владельцу")
                .lore("&7Благородный жест: вернуть все трофеи",
                      "&7проигравшему сопернику.",
                      "",
                      "&a▶ Нажмите для возврата")
                .build());
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
        int slot = event.getRawSlot();
        if (slot >= 45 && slot < 54) {
            event.setCancelled(true);
            if (slot == 49) {
                PlayerData data = plugin.getStatsManager().getPlayerData(player);
                data.getLoserLootItems().clear();
                plugin.getStatsManager().savePlayerData(player.getUniqueId());
                plugin.getMessageManager().sendRawMessage(player, "{prefix} &aВы благородно вернули все предметы сопернику!");
                player.closeInventory();
            }
            return;
        }

        if (slot >= 0 && slot < 45) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && !clicked.getType().isAir()) {
                PlayerData data = plugin.getStatsManager().getPlayerData(player);
                data.getLoserLootItems().remove(clicked);
                plugin.getStatsManager().savePlayerData(player.getUniqueId());
            }
        }
    }
}
