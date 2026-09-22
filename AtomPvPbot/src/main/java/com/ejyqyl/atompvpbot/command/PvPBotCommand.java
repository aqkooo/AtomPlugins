package com.ejyqyl.atompvpbot.command;

import com.ejyqyl.atompvpbot.ai.BotDifficulty;
import com.ejyqyl.atompvpbot.arena.Arena;
import com.ejyqyl.atompvpbot.arena.ArenaManager;
import com.ejyqyl.atompvpbot.config.ConfigManager;
import com.ejyqyl.atompvpbot.config.MessageManager;
import com.ejyqyl.atompvpbot.data.PlayerData;
import com.ejyqyl.atompvpbot.data.StatsManager;
import com.ejyqyl.atompvpbot.entity.PvPBotEntity;
import com.ejyqyl.atompvpbot.fight.FightManager;
import com.ejyqyl.atompvpbot.gui.MainBotMenu;
import com.ejyqyl.atompvpbot.gui.PlayerFightPreferences;
import com.ejyqyl.atompvpbot.gui.StatsMenu;
import com.ejyqyl.atompvpbot.kit.BotKit;
import com.ejyqyl.atompvpbot.kit.KitManager;
import com.ejyqyl.atompvpbot.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Universal command handler for /pvpbot (gui, arena, kit, fight, difficulty, stats, reload).
 *
 * @author ejyqyl
 */
public class PvPBotCommand implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final ArenaManager arenaManager;
    private final KitManager kitManager;
    private final FightManager fightManager;
    private final StatsManager statsManager;
    private final ConfigManager configManager;
    private final MessageManager messageManager;

    public PvPBotCommand(Plugin plugin, ArenaManager arenaManager, KitManager kitManager,
                         FightManager fightManager, StatsManager statsManager,
                         ConfigManager configManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.kitManager = kitManager;
        this.fightManager = fightManager;
        this.statsManager = statsManager;
        this.configManager = configManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("<#FF5555>Эта команда доступна только для игроков!"));
            return true;
        }

        // /pvpbot -> Open Main GUI
        if (args.length == 0) {
            new MainBotMenu(plugin, player, arenaManager, kitManager, fightManager, statsManager).open();
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "fight" -> handleFight(player);
            case "leave", "stop" -> handleLeave(player);
            case "create" -> handleCreate(player, args);
            case "remove" -> handleRemove(player);
            case "difficulty" -> handleDifficulty(player, args);
            case "arena" -> handleArena(player, args);
            case "kit" -> handleKit(player, args);
            case "stats" -> handleStats(player, args);
            case "reload" -> handleReload(player);
            default -> {
                player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>AtomPvPbot ≫</gradient> &7Неизвестная подкоманда. Используйте &e/pvpbot &7для открытия меню."));
            }
        }

        return true;
    }

    private void handleFight(Player player) {
        PlayerFightPreferences prefs = PlayerFightPreferences.get(player.getUniqueId());
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
            player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &cНет доступных арен!"));
            return;
        }

        BotKit kit = kitManager.getKit(prefs.getKitName());
        fightManager.startFight(player, prefs.getDifficulty(), prefs.getBehaviorMode(), kit, arena, prefs.isContinuousTraining());
        player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aБой успешно запущен на арене &e" + arena.getDisplayName()));
    }

    private void handleLeave(Player player) {
        if (!fightManager.isInFight(player)) {
            player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &cВы сейчас не находитесь в бою!"));
            return;
        }
        fightManager.stopFight(player, false);
        player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &eВы покинули тренировочный бой."));
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("atompvpbot.admin")) {
            player.sendMessage(ColorUtil.parse("<#FF5555>У вас нет прав на эту команду!"));
            return;
        }

        BotDifficulty diff = args.length > 1 ? BotDifficulty.fromString(args[1]) : BotDifficulty.NORMAL;
        BotKit kit = args.length > 2 ? kitManager.getKit(args[2]) : null;

        PvPBotEntity bot = new PvPBotEntity(plugin);
        bot.setDifficulty(diff);
        bot.setKit(kit);
        bot.setFightOpponent(player);
        bot.spawn(player.getLocation().add(player.getLocation().getDirection().multiply(3)));

        player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aБот успешно создан со сложностью " + diff.getDisplayName()));
    }

    private void handleRemove(Player player) {
        if (fightManager.isInFight(player)) {
            fightManager.stopFight(player, false);
            player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &eАктивный бот удален."));
        } else {
            player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &7Нет активных ботов рядом с вами."));
        }
    }

    private void handleDifficulty(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &7Используйте: &e/pvpbot difficulty <easy|normal|hard|expert|custom>"));
            return;
        }
        BotDifficulty diff = BotDifficulty.fromString(args[1]);
        PlayerFightPreferences.get(player.getUniqueId()).setDifficulty(diff);
        player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aСложность установлена на: " + diff.getDisplayName()));
    }

    private void handleArena(Player player, String[] args) {
        if (!player.hasPermission("atompvpbot.admin")) {
            player.sendMessage(ColorUtil.parse("<#FF5555>У вас нет прав на настройку арен!"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtil.parse("&7/pvpbot arena <create|delete|setspawn1|setspawn2|setpos1|setpos2|list> [имя]"));
            return;
        }

        String action = args[1].toLowerCase();
        String arenaName = args.length > 2 ? args[2] : null;

        if (action.equals("list")) {
            player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &7Список зарегистрированных арен:"));
            for (Arena a : arenaManager.getArenas()) {
                String status = a.isConfigured() ? (a.isInUse() ? "&cЗанята" : "&aГотова") : "&eНе настроена";
                player.sendMessage(ColorUtil.parse(" &8• &b" + a.getName() + " &8[" + status + "&8]"));
            }
            return;
        }

        if (arenaName == null) {
            player.sendMessage(ColorUtil.parse("<#FF5555>Укажите имя арены!"));
            return;
        }

        switch (action) {
            case "create" -> {
                Arena arena = arenaManager.createArena(arenaName);
                player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aАрена &e" + arena.getName() + " &aуспешно создана!"));
            }
            case "delete" -> {
                if (arenaManager.deleteArena(arenaName)) {
                    player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aАрена &e" + arenaName + " &aудалена!"));
                } else {
                    player.sendMessage(ColorUtil.parse("<#FF5555>Арена не найдена!"));
                }
            }
            case "setspawn1" -> {
                Arena arena = arenaManager.getArena(arenaName);
                if (arena == null) {
                    player.sendMessage(ColorUtil.parse("<#FF5555>Арена не найдена!"));
                    return;
                }
                arena.setSpawn1(player.getLocation());
                arenaManager.saveArenas();
                player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aТочка spawn1 для арены &e" + arenaName + " &aустановлена!"));
            }
            case "setspawn2" -> {
                Arena arena = arenaManager.getArena(arenaName);
                if (arena == null) {
                    player.sendMessage(ColorUtil.parse("<#FF5555>Арена не найдена!"));
                    return;
                }
                arena.setSpawn2(player.getLocation());
                arenaManager.saveArenas();
                player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aТочка spawn2 для арены &e" + arenaName + " &aустановлена!"));
            }
            case "setpos1" -> {
                Arena arena = arenaManager.getArena(arenaName);
                if (arena == null) {
                    player.sendMessage(ColorUtil.parse("<#FF5555>Арена не найдена!"));
                    return;
                }
                arena.setPos1(player.getLocation());
                arenaManager.saveArenas();
                player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aГраница pos1 для арены &e" + arenaName + " &aустановлена!"));
            }
            case "setpos2" -> {
                Arena arena = arenaManager.getArena(arenaName);
                if (arena == null) {
                    player.sendMessage(ColorUtil.parse("<#FF5555>Арена не найдена!"));
                    return;
                }
                arena.setPos2(player.getLocation());
                arenaManager.saveArenas();
                player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aГраница pos2 для арены &e" + arenaName + " &aустановлена!"));
            }
            default -> player.sendMessage(ColorUtil.parse("&7Неизвестное действие арены."));
        }
    }

    private void handleKit(Player player, String[] args) {
        if (!player.hasPermission("atompvpbot.admin")) {
            player.sendMessage(ColorUtil.parse("<#FF5555>У вас нет прав на управление наборами!"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtil.parse("&7/pvpbot kit <create|delete|give|list> [имя]"));
            return;
        }

        String action = args[1].toLowerCase();
        String kitName = args.length > 2 ? args[2] : null;

        if (action.equals("list")) {
            player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &7Список наборов:"));
            for (BotKit k : kitManager.getKits()) {
                player.sendMessage(ColorUtil.parse(" &8• &b" + k.getName() + " &7- " + k.getDisplayName()));
            }
            return;
        }

        if (kitName == null) {
            player.sendMessage(ColorUtil.parse("<#FF5555>Укажите имя набора!"));
            return;
        }

        switch (action) {
            case "create" -> {
                kitManager.createKitFromPlayer(kitName, player);
                player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aНабор &e" + kitName + " &aуспешно сохранен из вашего инвентаря!"));
            }
            case "delete" -> {
                if (kitManager.deleteKit(kitName)) {
                    player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aНабор &e" + kitName + " &aудален!"));
                } else {
                    player.sendMessage(ColorUtil.parse("<#FF5555>Набор не найден!"));
                }
            }
            case "give" -> {
                BotKit kit = kitManager.getKit(kitName);
                if (kit != null) {
                    kit.applyTo(player);
                    player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aВам выдан набор &e" + kit.getDisplayName()));
                } else {
                    player.sendMessage(ColorUtil.parse("<#FF5555>Набор не найден!"));
                }
            }
            default -> player.sendMessage(ColorUtil.parse("&7Неизвестное действие с набором."));
        }
    }

    private void handleStats(Player player, String[] args) {
        if (args.length > 1) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                PlayerData d = statsManager.getPlayerData(target);
                player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &7Статистика игрока &b" + target.getName() + ":"));
                player.sendMessage(ColorUtil.parse(" &7• Побед: &a" + d.getWins() + " &8| &7Поражений: &c" + d.getLosses()));
                player.sendMessage(ColorUtil.parse(" &7• Винрейт: &e" + String.format("%.1f", d.getWinRate()) + "%"));
                player.sendMessage(ColorUtil.parse(" &7• Серия: &6" + d.getWinStreak() + " &8(Лучшая: " + d.getBestStreak() + ")"));
                return;
            }
        }
        new StatsMenu(plugin, player, arenaManager, kitManager, fightManager, statsManager).open();
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("atompvpbot.admin")) {
            player.sendMessage(ColorUtil.parse("<#FF5555>У вас нет прав на перезагрузку плагина!"));
            return;
        }

        configManager.loadConfig();
        messageManager.loadMessages();
        arenaManager.loadArenas();
        kitManager.loadKits();
        player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aВсе конфигурации, арены и наборы перезагружены!"));
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("fight", "leave", "stop", "create", "remove", "difficulty", "arena", "kit", "stats", "reload"), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "difficulty" -> filter(List.of("easy", "normal", "hard", "expert", "custom"), args[1]);
                case "arena" -> filter(List.of("create", "delete", "setspawn1", "setspawn2", "setpos1", "setpos2", "list"), args[1]);
                case "kit" -> filter(List.of("create", "delete", "give", "list"), args[1]);
                case "create" -> filter(List.of("easy", "normal", "hard", "expert"), args[1]);
                default -> Collections.emptyList();
            };
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("arena")) {
                return filter(arenaManager.getArenas().stream().map(Arena::getName).toList(), args[2]);
            }
            if (args[0].equalsIgnoreCase("kit")) {
                return filter(kitManager.getKits().stream().map(BotKit::getName).toList(), args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String input) {
        List<String> res = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(input.toLowerCase())) {
                res.add(s);
            }
        }
        return res;
    }
}
