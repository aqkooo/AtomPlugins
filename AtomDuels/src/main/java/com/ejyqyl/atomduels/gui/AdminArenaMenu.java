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
 * In-game administration panel for managing duel arenas.
 * Authored by ejyqyl.
 */
public class AdminArenaMenu implements AtomMenu {

    private final AtomDuels plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<Arena> arenaList = new ArrayList<>();

    public AdminArenaMenu(AtomDuels plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 36, ColorUtil.parse("<gradient:#FF5555:#FFAA00>Админ-панель • Арены</gradient>"));
        build();
    }

    private void build() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        arenaList.clear();
        arenaList.addAll(plugin.getArenaManager().getArenas());

        int slot = 0;
        for (Arena arena : arenaList) {
            if (slot >= 27) break;

            inventory.setItem(slot, new ItemBuilder(Material.SANDSTONE)
                    .name("<#00B5FD>" + arena.getName())
                    .lore("&7Включена: " + (arena.isEnabled() ? "&aДа" : "&cНет"),
                          "&7Настроена: " + (arena.isConfigured() ? "&aДа" : "&cНет"),
                          "",
                          "&e▶ ЛКМ: Вкл/Выкл",
                          "&6▶ Shift+ЛКМ: Установить Spawn 1",
                          "&b▶ Shift+ПКМ: Установить Spawn 2",
                          "&c▶ Клавиша Q: Удалить арену")
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

        if (slot >= 0 && slot < arenaList.size()) {
            Arena arena = arenaList.get(slot);

            if (event.getClick().isKeyboardClick()) {
                plugin.getArenaManager().deleteArena(arena.getName());
                plugin.getMessageManager().sendRawMessage(player, "{prefix} &cАрена &e" + arena.getName() + " &cудалена.");
                build();
                return;
            }

            if (event.isShiftClick()) {
                if (event.isLeftClick()) {
                    arena.setSpawn1(player.getLocation().clone());
                    plugin.getArenaManager().saveArenas();
                    plugin.getMessageManager().sendRawMessage(player, "{prefix} &aSpawn 1 установлен для &e" + arena.getName());
                } else if (event.isRightClick()) {
                    arena.setSpawn2(player.getLocation().clone());
                    plugin.getArenaManager().saveArenas();
                    plugin.getMessageManager().sendRawMessage(player, "{prefix} &aSpawn 2 установлен для &e" + arena.getName());
                }
            } else {
                arena.setEnabled(!arena.isEnabled());
                plugin.getArenaManager().saveArenas();
            }
            build();
        }
    }
}
