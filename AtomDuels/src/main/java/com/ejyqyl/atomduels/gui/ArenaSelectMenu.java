package com.ejyqyl.atomduels.gui;

import com.ejyqyl.atomduels.AtomDuels;
import com.ejyqyl.atomduels.arena.Arena;
import com.ejyqyl.atomduels.util.ColorUtil;
import com.ejyqyl.atomduels.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Arena selection menu ("Где дерёмся").
 * Matches user screenshots with purple bar prefix and lime/red concrete blocks.
 * Authored by ejyqyl.
 */
public class ArenaSelectMenu implements AtomMenu {

    private final AtomDuels plugin;
    private final Player player;
    private final DuelSettingsMenu parentMenu;
    private final Inventory inventory;
    private final List<Arena> arenaList = new ArrayList<>();

    public ArenaSelectMenu(AtomDuels plugin, Player player, DuelSettingsMenu parentMenu) {
        this.plugin = plugin;
        this.player = player;
        this.parentMenu = parentMenu;
        this.inventory = Bukkit.createInventory(this, 36, ColorUtil.parse("<gradient:#00B5FD:#7670E5>Где дерёмся</gradient>"));
        build();
    }

    private void build() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        // Random / Any free arena
        inventory.setItem(10, new ItemBuilder(Material.COMPASS)
                .name("<gradient:#00FF88:#00B5FD>Любая свободная арена</gradient>")
                .lore("&7Автоматический выбор случайной",
                      "&7свободной боевой локации.",
                      " ",
                      "&e▶ Нажмите для выбора")
                .build());

        arenaList.clear();
        arenaList.addAll(plugin.getArenaManager().getArenas());

        int slot = 12;
        for (Arena arena : arenaList) {
            if (slot > 31) break;
            if (slot == 17 || slot == 18) slot = 19;

            boolean free = arena.isEnabled() && !arena.isInUse() && arena.isConfigured();
            Material mat = free ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
            String status = free ? "&aСвободна" : "&cЗанята в бою";

            // Purple bar prefix matching screenshot
            String displayName = "<#A335EE>| &f" + arena.getDisplayName();

            inventory.setItem(slot, new ItemBuilder(mat)
                    .name(displayName)
                    .lore("&7Статус арены: " + status,
                          "",
                          free ? "&e▶ Нажмите для выбора" : "&cАрена в данный момент недоступна")
                    .build());
            slot++;
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
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 10) {
            parentMenu.getRuleSet().setArenaName(null);
            parentMenu.build();
            parentMenu.open();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && clicked.getType() == Material.LIME_CONCRETE) {
            int index = slot - 12;
            if (slot >= 19) index -= 2;
            if (index >= 0 && index < arenaList.size()) {
                Arena arena = arenaList.get(index);
                parentMenu.getRuleSet().setArenaName(arena.getName());
                parentMenu.build();
                parentMenu.open();
            }
        }
    }
}
