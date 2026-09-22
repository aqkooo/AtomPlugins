package com.ejyqyl.atombot.gui;

import com.ejyqyl.atombot.AtomBot;
import com.ejyqyl.atombot.entity.TrainingBot;
import com.ejyqyl.atombot.model.BotSettings;
import com.ejyqyl.atombot.util.ColorUtil;
import com.ejyqyl.atombot.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * 54-slot Training NPC Bot configuration GUI for AtomBot.
 * Implements Настройки_бота.yml with zero bold &l clutter and hideFlags support.
 *
 * @author ejyqyl
 */
public class BotMenu implements InventoryHolder {

    private final AtomBot plugin;
    private final Player player;
    private final Inventory inventory;

    public BotMenu(AtomBot plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.parse("<gradient:#00B5FD:#7670E5>Настройки бота</gradient>"));
        build();
    }

    public void build() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").hideFlags().build();
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
                .hideFlags()
                .build());

        // Slot 11: Helmet
        Material helmIcon = settings.getHelmet() != BotSettings.ArmorTier.NONE ? settings.getHelmet().getHelmet() : Material.LEATHER_HELMET;
        inventory.setItem(11, new ItemBuilder(helmIcon)
                .name("<gradient:#F0C4CD:#E598A8>Шлем</gradient>")
                .lore(" ",
                      "&7ℹ &fМатериал: <gradient:#FF5B29:#FFAA00>" + settings.getHelmet().getDisplayName() + "</gradient>",
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .hideFlags()
                .build());

        // Slot 15: Follow Owner / Movement (Lead)
        String followStatus = settings.isFollowOwner() ? "&aВключено" : "&cВыключено";
        inventory.setItem(15, new ItemBuilder(Material.LEAD)
                .name("<gradient:#F0C4CD:#E598A8>Следовать за владельцем</gradient>")
                .lore(" ",
                      "&7ℹ &fСтатус: " + followStatus,
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .hideFlags()
                .build());

        // Slot 20: Chestplate
        Material chestIcon = settings.getChestplate() != BotSettings.ArmorTier.NONE ? settings.getChestplate().getChestplate() : Material.LEATHER_CHESTPLATE;
        inventory.setItem(20, new ItemBuilder(chestIcon)
                .name("<gradient:#F0C4CD:#E598A8>Нагрудник</gradient>")
                .lore(" ",
                      "&7ℹ &fМатериал: <gradient:#FF5B29:#FFAA00>" + settings.getChestplate().getDisplayName() + "</gradient>",
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .hideFlags()
                .build());

        // Slot 24: Shield
        String shieldStatus = settings.isUseShield() ? "&aВключено" : "&cВыключено";
        inventory.setItem(24, new ItemBuilder(Material.SHIELD)
                .name("<gradient:#F0C4CD:#E598A8>Использовать щит</gradient>")
                .lore(" ",
                      "&7ℹ &fСтатус: " + shieldStatus,
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .hideFlags()
                .build());

        // Slot 29: Leggings
        Material legIcon = settings.getLeggings() != BotSettings.ArmorTier.NONE ? settings.getLeggings().getLeggings() : Material.LEATHER_LEGGINGS;
        inventory.setItem(29, new ItemBuilder(legIcon)
                .name("<gradient:#F0C4CD:#E598A8>Штаны</gradient>")
                .lore(" ",
                      "&7ℹ &fМатериал: <gradient:#FF5B29:#FFAA00>" + settings.getLeggings().getDisplayName() + "</gradient>",
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .hideFlags()
                .build());

        // Slot 33: Slow Falling (Feather)
        String slowStatus = settings.isSlowFalling() ? "&aВключено" : "&cВыключено";
        inventory.setItem(33, new ItemBuilder(Material.FEATHER)
                .name("<gradient:#F0C4CD:#E598A8>Плавное падение</gradient>")
                .lore(" ",
                      "&7ℹ &fСтатус: " + slowStatus,
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .hideFlags()
                .build());

        // Slot 38: Boots
        Material bootIcon = settings.getBoots() != BotSettings.ArmorTier.NONE ? settings.getBoots().getBoots() : Material.LEATHER_BOOTS;
        inventory.setItem(38, new ItemBuilder(bootIcon)
                .name("<gradient:#F0C4CD:#E598A8>Ботинки</gradient>")
                .lore(" ",
                      "&7ℹ &fМатериал: <gradient:#FF5B29:#FFAA00>" + settings.getBoots().getDisplayName() + "</gradient>",
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .hideFlags()
                .build());

        // Slot 42: Attack Player (Iron Sword)
        String attackStatus = settings.isAttackPlayer() ? "&aВключено" : "&cВыключено";
        inventory.setItem(42, new ItemBuilder(Material.IRON_SWORD)
                .name("<gradient:#F0C4CD:#E598A8>Атаковать игрока</gradient>")
                .lore(" ",
                      "&7ℹ &fСтатус: " + attackStatus,
                      " ",
                      "&6▶ &eНажмите, чтобы переключить")
                .hideFlags()
                .build());

        // Slot 48: Spawn Bot (Lime Dye)
        inventory.setItem(48, new ItemBuilder(Material.LIME_DYE)
                .name("<gradient:#00FF88:#00B5FD>Призвать бота</gradient>")
                .lore(" ",
                      "&7ℹ &fПризвать тренировочного NPC-бота перед собой",
                      " ",
                      "&6▶ &eНажмите, чтобы призвать")
                .hideFlags()
                .build());

        // Slot 50: Remove Bot (Red Dye)
        inventory.setItem(50, new ItemBuilder(Material.RED_DYE)
                .name("<gradient:#FF5555:#FFAA00>Удалить бота</gradient>")
                .lore(" ",
                      "&7ℹ &fУдалить вашего активного бота",
                      " ",
                      "&6▶ &eНажмите, чтобы удалить")
                .hideFlags()
                .build());
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

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
                plugin.getBotManager().spawnBot(player);
                plugin.sendMessage(player, "messages.spawned");
            }
            case 50 -> {
                player.closeInventory();
                boolean removed = plugin.getBotManager().removeBot(player.getUniqueId());
                if (removed) {
                    plugin.sendMessage(player, "messages.removed");
                } else {
                    plugin.sendMessage(player, "messages.no-bot");
                }
            }
        }

        if (settingChanged) {
            TrainingBot activeBot = plugin.getBotManager().getBot(player.getUniqueId());
            if (activeBot != null) {
                activeBot.applySettings(settings);
            }
            build();
        }
    }
}
