package com.ejyqyl.atomduels.gui;

import com.ejyqyl.atomduels.AtomDuels;
import com.ejyqyl.atomduels.util.ColorUtil;
import com.ejyqyl.atomduels.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Main navigation hub for AtomDuels (/duels).
 * Designed with clean gradients, readable text, and zero bold &l clutter.
 * Authored by ejyqyl (https://github.com/aqkooo).
 */
public class MainMenu implements AtomMenu {

    private final AtomDuels plugin;
    private final Player player;
    private final Inventory inventory;

    public MainMenu(AtomDuels plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 36, ColorUtil.parse("<gradient:#00B5FD:#7670E5>AtomDuels ✦ Меню</gradient>"));
        build();
    }

    private void build() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        // Quick Queue
        ItemStack queueItem = new ItemBuilder(Material.COMPASS)
                .name("<gradient:#00FF88:#00B5FD>Быстрый поиск боя</gradient>")
                .lore("&7Встаньте в очередь матчмейкинга",
                      "&7по системе Elo-рейтинга.",
                      " ",
                      "&e▶ Нажмите, чтобы выбрать режим")
                .hideFlags()
                .build();
        inventory.setItem(10, queueItem);

        // Challenge player
        ItemStack challengeItem = new ItemBuilder(Material.DIAMOND_SWORD)
                .name("<gradient:#00B5FD:#7670E5>Вызвать игрока</gradient>")
                .lore("&7Создайте персональный вызов",
                      "&7с гибкой настройкой правил и ставок.",
                      " ",
                      "&e▶ Нажмите или введите /duels duel <ник>")
                .hideFlags()
                .build();
        inventory.setItem(12, challengeItem);

        // Open challenges
        ItemStack openItem = new ItemBuilder(Material.BOOK)
                .name("<gradient:#FFAA00:#FF5555>Открытые вызовы</gradient>")
                .lore("&7Список вызовов, доступных",
                      "&7для принятия любым игроком.",
                      " ",
                      "&e▶ Нажмите для просмотра")
                .hideFlags()
                .build();
        inventory.setItem(14, openItem);

        // Bot fight & Training
        ItemStack botItem = new ItemBuilder(Material.IRON_HELMET)
                .name("<gradient:#FF5555:#FFAA00>Тренировочный NPC-бот</gradient>")
                .lore("&7Настройка поведения, щита, падения,",
                      "&7брони и спавн бота для тренировок.",
                      " ",
                      "&e▶ Нажмите для открытия настроек")
                .hideFlags()
                .build();
        inventory.setItem(16, botItem);

        // Stats
        ItemStack statsItem = new ItemBuilder(Material.PLAYER_HEAD)
                .name("<gradient:#00E5FF:#00B5FD>Ваша статистика</gradient>")
                .lore("&7Просмотр рейтинга, винстриков,",
                      "&7истории и выигранных средств.",
                      " ",
                      "&e▶ Нажмите для открытия")
                .hideFlags()
                .build();
        inventory.setItem(20, statsItem);

        // Leaderboard Top
        ItemStack topItem = new ItemBuilder(Material.NETHER_STAR)
                .name("<gradient:#FFD700:#FFAA00>Топ игроков</gradient>")
                .lore("&7Таблица лидеров сервера по Elo.",
                      " ",
                      "&e▶ Нажмите для просмотра")
                .hideFlags()
                .build();
        inventory.setItem(22, topItem);

        // Claim Items
        ItemStack claimItem = new ItemBuilder(Material.CHEST)
                .name("<gradient:#A335EE:#7670E5>Хранилище предметов</gradient>")
                .lore("&7Заберите предметы, которые не поместились",
                      "&7в инвентарь после завершения дуэлей.",
                      " ",
                      "&e▶ Нажмите, чтобы открыть")
                .hideFlags()
                .build();
        inventory.setItem(24, claimItem);
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

        switch (slot) {
            case 10 -> new QueueMenu(plugin, player).open();
            case 12 -> {
                player.closeInventory();
                plugin.getMessageManager().sendRawMessage(player, "{prefix} &fИспользуйте команду: &e/duels duel <ник>");
            }
            case 14 -> new OpenChallengesMenu(plugin, player).open();
            case 16 -> new BotSettingsMenu(plugin, player).open();
            case 20 -> new StatsMenu(plugin, player, player.getUniqueId()).open();
            case 22 -> {
                player.closeInventory();
                player.performCommand("duels top");
            }
            case 24 -> new ClaimMenu(plugin, player).open();
        }
    }
}
