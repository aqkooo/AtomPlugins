package com.ejyqyl.atompvpbot.gui;

import com.ejyqyl.atompvpbot.arena.Arena;
import com.ejyqyl.atompvpbot.arena.ArenaManager;
import com.ejyqyl.atompvpbot.data.PlayerData;
import com.ejyqyl.atompvpbot.data.StatsManager;
import com.ejyqyl.atompvpbot.fight.FightManager;
import com.ejyqyl.atompvpbot.kit.BotKit;
import com.ejyqyl.atompvpbot.kit.KitManager;
import com.ejyqyl.atompvpbot.util.ColorUtil;
import com.ejyqyl.atompvpbot.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 54-slot central control dashboard for AtomPvPbot.
 *
 * @author ejyqyl
 */
public class MainBotMenu extends AtomMenu {

    private final Plugin plugin;
    private final ArenaManager arenaManager;
    private final KitManager kitManager;
    private final FightManager fightManager;
    private final StatsManager statsManager;

    public MainBotMenu(Plugin plugin, Player player, ArenaManager arenaManager, KitManager kitManager,
                       FightManager fightManager, StatsManager statsManager) {
        super(player, 54, "<gradient:#00B5FD:#7670E5>AtomPvPbot</gradient> &8| &7Главное меню");
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.kitManager = kitManager;
        this.fightManager = fightManager;
        this.statsManager = statsManager;
    }

    @Override
    public void initialize() {
        ItemStack border = createGlass(Material.GRAY_STAINED_GLASS_PANE);
        fillBorder(border);
        fillEmpty(createGlass(Material.BLACK_STAINED_GLASS_PANE));

        PlayerFightPreferences prefs = PlayerFightPreferences.get(player.getUniqueId());
        PlayerData data = statsManager.getPlayerData(player);

        // 1. ⚔ Начать бой (Slot 13)
        List<String> startLore = new ArrayList<>();
        startLore.add("&8Параметры сессии:");
        startLore.add(" &7• Сложность: " + prefs.getDifficulty().getDisplayName());
        startLore.add(" &7• Поведение: " + prefs.getBehaviorMode().getDisplayName());
        startLore.add(" &7• Набор: &b" + prefs.getKitName());
        startLore.add(" &7• Арена: &e" + (prefs.getArenaName() != null ? prefs.getArenaName() : "Любая свободная"));
        startLore.add(" &7• Бесконечная тренировка: " + (prefs.isContinuousTraining() ? "&aВключена" : "&cВыключена"));
        startLore.add("");
        startLore.add("&eНажмите, чтобы начать тренировку!");

        ItemStack startItem = new ItemBuilder(Material.NETHERITE_SWORD)
                .name("<gradient:#FF4B2B:#FF416C>⚔ Начать тренировку</gradient>")
                .lore(startLore)
                .hideFlags()
                .build();

        setItem(13, startItem, e -> {
            player.closeInventory();
            launchFight(prefs);
        });

        // 2. 🎯 Сложность (Slot 20)
        ItemStack diffItem = new ItemBuilder(Material.TARGET)
                .name("<gradient:#FFFF55:#FFAA00>🎯 Сложность бота</gradient>")
                .lore(
                        "&7Текущая сложность: " + prefs.getDifficulty().getDisplayName(),
                        "&7CPS: &f" + prefs.getDifficulty().getCps() + " &8| &7Аим: &f" + (int)(prefs.getDifficulty().getAimAccuracy() * 100) + "%",
                        "",
                        "&eНажмите для выбора сложности"
                )
                .hideFlags()
                .build();

        setItem(20, diffItem, e -> {
            new DifficultySelectMenu(plugin, player, arenaManager, kitManager, fightManager, statsManager).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
        });

        // 3. 🤖 Режим поведения (Slot 22)
        ItemStack modeItem = new ItemBuilder(Material.IRON_HELMET)
                .name("<gradient:#36D1DC:#5B86E5>🤖 Режим поведения</gradient>")
                .lore(
                        "&7Текущий режим: " + prefs.getBehaviorMode().getDisplayName(),
                        "&8" + prefs.getBehaviorMode().getDescription(),
                        "",
                        "&eНажмите для смены стиля боя"
                )
                .hideFlags()
                .build();

        setItem(22, modeItem, e -> {
            new BehaviorModeSelectMenu(plugin, player, arenaManager, kitManager, fightManager, statsManager).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
        });

        // 4. 🛡 Выбрать кит (Slot 24)
        ItemStack kitItem = new ItemBuilder(Material.DIAMOND_CHESTPLATE)
                .name("<gradient:#00B5FD:#7670E5>🛡 Выбрать набор экипировки</gradient>")
                .lore(
                        "&7Текущий набор: &b" + prefs.getKitName(),
                        "&7Доступно наборов: &f" + kitManager.getKits().size(),
                        "",
                        "&eНажмите для выбора кита"
                )
                .hideFlags()
                .build();

        setItem(24, kitItem, e -> {
            new KitSelectMenu(plugin, player, arenaManager, kitManager, fightManager, statsManager).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
        });

        // 5. 🗺 Арена (Slot 30)
        ItemStack arenaItem = new ItemBuilder(Material.MAP)
                .name("<gradient:#F7971E:#FFD200>🗺 Выбрать арену</gradient>")
                .lore(
                        "&7Текущая арена: &e" + (prefs.getArenaName() != null ? prefs.getArenaName() : "Любая свободная"),
                        "&7Всего арен: &f" + arenaManager.getArenas().size(),
                        "",
                        "&eНажмите для выбора арены"
                )
                .hideFlags()
                .build();

        setItem(30, arenaItem, e -> {
            new ArenaSelectMenu(plugin, player, arenaManager, kitManager, fightManager, statsManager).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
        });

        // 6. ⚙ Настройки (Slot 32)
        ItemStack settingsItem = new ItemBuilder(Material.COMPARATOR)
                .name("<gradient:#A8FF78:#78FFD6>⚙ Настройки тренировки</gradient>")
                .lore(
                        "&7Бесконечный бой: " + (prefs.isContinuousTraining() ? "&aВкл" : "&cВыкл"),
                        "&7Авто-перезапуск раунда: &a3 секунды",
                        "",
                        "&eНажмите для изменения параметров"
                )
                .hideFlags()
                .build();

        setItem(32, settingsItem, e -> {
            new SettingsMenu(plugin, player, arenaManager, kitManager, fightManager, statsManager).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
        });

        // 7. 📊 Статистика (Slot 40)
        ItemStack skullItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skullItem.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(ColorUtil.colorize("<gradient:#00B5FD:#7670E5>📊 Ваша статистика</gradient>"));
            List<String> lore = new ArrayList<>();
            lore.add("&7Побед: &a" + data.getWins());
            lore.add("&7Поражений: &c" + data.getLosses());
            lore.add("&7Винрейт: &e" + String.format("%.1f", data.getWinRate()) + "%");
            lore.add("&7Серия побед: &6" + data.getWinStreak() + " &8(Лучшая: " + data.getBestStreak() + ")");
            lore.add("");
            lore.add("&eНажмите для детальной статистики");
            skullMeta.setLore(lore.stream().map(ColorUtil::colorize).toList());
            skullItem.setItemMeta(skullMeta);
        }

        setItem(40, skullItem, e -> {
            new StatsMenu(plugin, player, arenaManager, kitManager, fightManager, statsManager).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
        });

        // Закрыть (Slot 49)
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name("&cЗакрыть меню")
                .hideFlags()
                .build();

        setItem(49, closeItem, e -> player.closeInventory());
    }

    private void launchFight(PlayerFightPreferences prefs) {
        Arena arena;
        if (prefs.getArenaName() != null) {
            arena = arenaManager.getArena(prefs.getArenaName());
            if (arena == null || !arena.isEnabled() || arena.isInUse() || !arena.isConfigured()) {
                arena = arenaManager.getAvailableArena();
            }
        } else {
            arena = arenaManager.getAvailableArena();
        }

        if (arena == null) {
            player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &cНет доступных арен! Создайте арену через &e/pvpbot arena create"));
            return;
        }

        BotKit kit = kitManager.getKit(prefs.getKitName());
        if (kit == null) {
            kit = kitManager.getKits().stream().findFirst().orElse(null);
        }

        fightManager.startFight(player, prefs.getDifficulty(), prefs.getBehaviorMode(), kit, arena, prefs.isContinuousTraining());
        player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aБой запущен на арене &e" + arena.getDisplayName()));
    }
}
