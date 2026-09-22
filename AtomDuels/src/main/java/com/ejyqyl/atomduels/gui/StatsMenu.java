package com.ejyqyl.atomduels.gui;

import com.ejyqyl.atomduels.AtomDuels;
import com.ejyqyl.atomduels.data.PlayerData;
import com.ejyqyl.atomduels.util.ColorUtil;
import com.ejyqyl.atomduels.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/**
 * Player statistics profile menu ("Статистика • <ник>").
 * Recreates the user's uploaded mockup showing head, money stats, and calibration info.
 * Authored by ejyqyl.
 */
public class StatsMenu implements AtomMenu {

    private final AtomDuels plugin;
    private final Player viewer;
    private final UUID targetUuid;
    private final Inventory inventory;

    public StatsMenu(AtomDuels plugin, Player viewer, UUID targetUuid) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.targetUuid = targetUuid;

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        String name = target.getName() != null ? target.getName() : "Игрок";
        this.inventory = Bukkit.createInventory(this, 27, ColorUtil.parse("<gradient:#00B5FD:#7670E5>Статистика • " + name + "</gradient>"));
        build(target, name);
    }

    private void build(OfflinePlayer target, String name) {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        PlayerData data = plugin.getStatsManager().getPlayerData(targetUuid, name);

        // Player Head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        head.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(target));
        ItemBuilder headBuilder = new ItemBuilder(head)
                .name("<#00B5FD>Профиль &f" + name)
                .lore("&7Рейтинг (Elo): &b" + data.getElo() + " &8(" + data.getRank().getFormattedName() + "&8)",
                      "&7Побед: &a" + data.getWins() + " &8| &7Поражений: &c" + data.getLosses() + " &8| &7Ничьих: &e" + data.getDraws(),
                      "&7Винрейт: &f" + String.format("%.1f", data.getWinRate()) + "%",
                      "&7Текущая серия побед: &e" + data.getWinStreak(),
                      "&7Лучшая серия побед: &6" + data.getBestStreak());
        inventory.setItem(11, headBuilder.build());

        // Economy / Money
        double profit = data.getNetProfit();
        String profitColor = profit >= 0 ? "&a+" : "&c";
        inventory.setItem(13, new ItemBuilder(Material.GOLD_INGOT)
                .name("<#FFD700>Деньги и Ставки")
                .lore("&7Выиграно: &a+" + String.format("%.2f", data.getMoneyWon()) + " $",
                      "&7Проиграно: &c-" + String.format("%.2f", data.getMoneyLost()) + " $",
                      "&7Чистая прибыль: " + profitColor + String.format("%.2f", profit) + " $")
                .build());

        // Calibration
        int calib = Math.min(10, data.getCalibrationMatches());
        inventory.setItem(15, new ItemBuilder(Material.BEACON)
                .name("<#00FF88>Калибровка рейтинга")
                .lore("&7Матчей сыграно: &e" + calib + " &8/ &710",
                      "&7Статус: " + (calib >= 10 ? "&aЗавершена" : "&eВ процессе"),
                      "",
                      "&7Во время калибровки за победу и",
                      "&7поражение начисляется больше Elo.")
                .build());
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
