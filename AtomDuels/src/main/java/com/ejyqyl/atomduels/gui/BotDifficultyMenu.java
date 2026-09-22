package com.ejyqyl.atomduels.gui;

import com.ejyqyl.atomduels.AtomDuels;
import com.ejyqyl.atomduels.bot.BotDifficulty;
import com.ejyqyl.atomduels.rules.RuleSet;
import com.ejyqyl.atomduels.util.ColorUtil;
import com.ejyqyl.atomduels.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Menu to select bot difficulty.
 * Authored by ejyqyl.
 */
public class BotDifficultyMenu implements AtomMenu {

    private final AtomDuels plugin;
    private final Player player;
    private final Inventory inventory;

    public BotDifficultyMenu(AtomDuels plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, ColorUtil.parse("<gradient:#00B5FD:#7670E5>Выбор сложности бота</gradient>"));
        build();
    }

    private void build() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(11, new ItemBuilder(Material.LIME_DYE)
                .name("<#55FF55>Легкий")
                .lore("&7Реакция: &f700ms", "&7Точность: &f65%", "&7Комбо/Зелья: &fНизкая", "", "&e▶ Нажмите для боя")
                .build());

        inventory.setItem(12, new ItemBuilder(Material.YELLOW_DYE)
                .name("<#FFFF55>Нормальный")
                .lore("&7Реакция: &f450ms", "&7Точность: &f80%", "&7Комбо/Зелья: &fСредняя", "", "&e▶ Нажмите для боя")
                .build());

        inventory.setItem(13, new ItemBuilder(Material.ORANGE_DYE)
                .name("<#FFAA00>Сложный")
                .lore("&7Реакция: &f250ms", "&7Точность: &f90%", "&7Комбо/Зелья: &fВысокая", "", "&e▶ Нажмите для боя")
                .build());

        inventory.setItem(14, new ItemBuilder(Material.RED_DYE)
                .name("<#FF5555>Эксперт")
                .lore("&7Реакция: &f120ms", "&7Точность: &f98%", "&7Комбо/Зелья: &fМаксимальная", "", "&e▶ Нажмите для боя")
                .build());

        inventory.setItem(15, new ItemBuilder(Material.PURPLE_DYE)
                .name("<#A335EE>Пользовательский")
                .lore("&7Сбалансированные параметры", "&7для тренировки индивидуальных навыков.", "", "&e▶ Нажмите для боя")
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
        event.setCancelled(true);
        int slot = event.getRawSlot();

        BotDifficulty diff = switch (slot) {
            case 11 -> BotDifficulty.EASY;
            case 12 -> BotDifficulty.NORMAL;
            case 13 -> BotDifficulty.HARD;
            case 14 -> BotDifficulty.EXPERT;
            case 15 -> BotDifficulty.CUSTOM;
            default -> null;
        };

        if (diff != null) {
            player.closeInventory();
            RuleSet ruleSet = new RuleSet();
            ruleSet.setKitName("Classic");
            plugin.getDuelManager().startBotDuel(player, diff, ruleSet);
        }
    }
}
