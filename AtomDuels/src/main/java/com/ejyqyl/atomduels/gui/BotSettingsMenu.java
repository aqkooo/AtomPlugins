package com.ejyqyl.atomduels.gui;

import com.ejyqyl.atomduels.AtomDuels;
import com.ejyqyl.atomduels.bot.BotSettings;
import com.ejyqyl.atomduels.util.ColorUtil;
import com.ejyqyl.atomduels.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Interactive bot configuration menu matching the user's YAML specification.
 * Completely free of &l bold tags, featuring clean gradients and clear descriptions.
 * Authored by ejyqyl (https://github.com/aqkooo).
 */
public class BotSettingsMenu implements AtomMenu {

    private final AtomDuels plugin;
    private final Player player;
    private final Inventory inventory;

    public BotSettingsMenu(AtomDuels plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.parse("<gradient:#00B5FD:#7670E5>Настройки бота</gradient>"));
        build();
    }

    public void build() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        BotSettings settings = plugin.getBotManager().getOrCreateSettings(player.getUniqueId());

        // Slot 10: Blast Resistance (Book / Enchanted Book)
        Material blastMat = settings.isBlastResistance() ? Material.ENCHANTED_BOOK : Material.BOOK;
        String blastStatus = settings.isBlastResistance() ? "&aВключено" : "&cВыключено";
        inventory.setItem(10, new ItemBuilder(blastMat)
                .name("<gradient:#F0C4CD:#E598A8>Взрывоустойчивость</gradient>")
                .lore(" ",
                      "&7ℹ &fСтатус: " + blastStatus,
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .build());

        // Slot 11: Helmet
        Material helmIcon = settings.getHelmet() != BotSettings.ArmorTier.NONE ? settings.getHelmet().getHelmet() : Material.LEATHER_HELMET;
        inventory.setItem(11, new ItemBuilder(helmIcon)
                .name("<gradient:#F0C4CD:#E598A8>Шлем</gradient>")
                .lore(" ",
                      "&7ℹ &fМатериал: <gradient:#FF5B29:#FFAA00>" + settings.getHelmet().getDisplayName() + "</gradient>",
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .build());

        // Slot 15: Follow Owner / Movement (Lead)
        String followStatus = settings.isFollowOwner() ? "&aВключено" : "&cВыключено";
        inventory.setItem(15, new ItemBuilder(Material.LEAD)
                .name("<gradient:#F0C4CD:#E598A8>Следовать за владельцем</gradient>")
                .lore(" ",
                      "&7ℹ &fСтатус: " + followStatus,
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .build());

        // Slot 20: Chestplate
        Material chestIcon = settings.getChestplate() != BotSettings.ArmorTier.NONE ? settings.getChestplate().getChestplate() : Material.LEATHER_CHESTPLATE;
        inventory.setItem(20, new ItemBuilder(chestIcon)
                .name("<gradient:#F0C4CD:#E598A8>Нагрудник</gradient>")
                .lore(" ",
                      "&7ℹ &fМатериал: <gradient:#FF5B29:#FFAA00>" + settings.getChestplate().getDisplayName() + "</gradient>",
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .build());

        // Slot 24: Shield
        String shieldStatus = settings.isUseShield() ? "&aВключено" : "&cВыключено";
        inventory.setItem(24, new ItemBuilder(Material.SHIELD)
                .name("<gradient:#F0C4CD:#E598A8>Использовать щит</gradient>")
                .lore(" ",
                      "&7ℹ &fСтатус: " + shieldStatus,
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .build());

        // Slot 29: Leggings
        Material legIcon = settings.getLeggings() != BotSettings.ArmorTier.NONE ? settings.getLeggings().getLeggings() : Material.LEATHER_LEGGINGS;
        inventory.setItem(29, new ItemBuilder(legIcon)
                .name("<gradient:#F0C4CD:#E598A8>Штаны</gradient>")
                .lore(" ",
                      "&7ℹ &fМатериал: <gradient:#FF5B29:#FFAA00>" + settings.getLeggings().getDisplayName() + "</gradient>",
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .build());

        // Slot 33: Slow Falling (Feather)
        String slowStatus = settings.isSlowFalling() ? "&aВключено" : "&cВыключено";
        inventory.setItem(33, new ItemBuilder(Material.FEATHER)
                .name("<gradient:#F0C4CD:#E598A8>Плавное падение</gradient>")
                .lore(" ",
                      "&7ℹ &fСтатус: " + slowStatus,
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .build());

        // Slot 38: Boots
        Material bootIcon = settings.getBoots() != BotSettings.ArmorTier.NONE ? settings.getBoots().getBoots() : Material.LEATHER_BOOTS;
        inventory.setItem(38, new ItemBuilder(bootIcon)
                .name("<gradient:#F0C4CD:#E598A8>Ботинки</gradient>")
                .lore(" ",
                      "&7ℹ &fМатериал: <gradient:#FF5B29:#FFAA00>" + settings.getBoots().getDisplayName() + "</gradient>",
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .build());

        // Slot 42: Attack Player (Iron Sword)
        String attackStatus = settings.isAttackPlayer() ? "&aВключено" : "&cВыключено";
        inventory.setItem(42, new ItemBuilder(Material.IRON_SWORD)
                .name("<gradient:#F0C4CD:#E598A8>Атаковать игрока</gradient>")
                .lore(" ",
                      "&7ℹ &fСтатус: " + attackStatus,
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .build());

        // Slot 48: Spawn Bot (Lime Dye)
        inventory.setItem(48, new ItemBuilder(Material.LIME_DYE)
                .name("<gradient:#00FF88:#00B5FD>Призвать бота</gradient>")
                .lore(" ",
                      "&7ℹ &fПризвать тренировочного NPC-бота перед собой",
                      " ",
                      "&6▶ &eНажмите, чтобы призвать")
                .build());

        // Slot 50: Remove Bot (Red Dye)
        inventory.setItem(50, new ItemBuilder(Material.RED_DYE)
                .name("<gradient:#FF5555:#FFAA00>Удалить бота</gradient>")
                .lore(" ",
                      "&7ℹ &fУдалить вашего активного бота",
                      " ",
                      "&6▶ &eНажмите, чтобы удалить")
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

        BotSettings settings = plugin.getBotManager().getOrCreateSettings(player.getUniqueId());

        boolean settingChanged = false;
        switch (slot) {
            case 10 -> {
                settings.toggleBlastResistance();
                settingChanged = true;
            }
            case 11 -> {
                settings.nextHelmet();
                settingChanged = true;
            }
            case 15 -> {
                settings.toggleFollowOwner();
                settingChanged = true;
            }
            case 20 -> {
                settings.nextChestplate();
                settingChanged = true;
            }
            case 24 -> {
                settings.toggleUseShield();
                settingChanged = true;
            }
            case 29 -> {
                settings.nextLeggings();
                settingChanged = true;
            }
            case 33 -> {
                settings.toggleSlowFalling();
                settingChanged = true;
            }
            case 38 -> {
                settings.nextBoots();
                settingChanged = true;
            }
            case 42 -> {
                settings.toggleAttackPlayer();
                settingChanged = true;
            }
            case 48 -> {
                player.closeInventory();
                plugin.getBotManager().spawnTrainingBot(player, settings);
                plugin.getMessageManager().sendRawMessage(player, "{prefix} &aТренировочный NPC-бот успешно призван перед вами!");
            }
            case 50 -> {
                player.closeInventory();
                boolean removed = plugin.getBotManager().removeTrainingBot(player.getUniqueId());
                if (removed) {
                    plugin.getMessageManager().sendRawMessage(player, "{prefix} &cВаш тренировочный бот удалён.");
                } else {
                    plugin.getMessageManager().sendRawMessage(player, "{prefix} &7У вас нет активного бота.");
                }
            }
        }

        if (settingChanged) {
            com.ejyqyl.atomduels.bot.DuelBot activeBot = plugin.getBotManager().getTrainingBot(player.getUniqueId());
            if (activeBot != null) {
                activeBot.applySettings(settings);
            }
            build();
        }
    }
}
