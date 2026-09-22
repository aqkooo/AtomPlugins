package com.ejyqyl.atomduels.command;

import com.ejyqyl.atomduels.AtomDuels;
import com.ejyqyl.atomduels.arena.Arena;
import com.ejyqyl.atomduels.data.PlayerData;
import com.ejyqyl.atomduels.duel.ActiveDuel;
import com.ejyqyl.atomduels.gui.AdminArenaMenu;
import com.ejyqyl.atomduels.gui.AdminKitMenu;
import com.ejyqyl.atomduels.gui.BotDifficultyMenu;
import com.ejyqyl.atomduels.gui.ClaimMenu;
import com.ejyqyl.atomduels.gui.DuelSettingsMenu;
import com.ejyqyl.atomduels.gui.LootMenu;
import com.ejyqyl.atomduels.gui.MainMenu;
import com.ejyqyl.atomduels.gui.OpenChallengesMenu;
import com.ejyqyl.atomduels.gui.QueueMenu;
import com.ejyqyl.atomduels.gui.StatsMenu;
import com.ejyqyl.atomduels.kit.Kit;
import com.ejyqyl.atomduels.rules.RuleSet;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Master command handler for /duels and /duel with 16 subcommands and tab completions.
 * Authored by ejyqyl.
 */
public class DuelsCommand implements CommandExecutor, TabCompleter {

    private final AtomDuels plugin;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "duel", "accept", "deny", "queue", "open", "bot", "stats", "top",
            "surrender", "spectator", "claim", "loot", "arena", "kit", "reload", "help"
    );

    public DuelsCommand(AtomDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                plugin.reloadAll();
                sender.sendMessage("AtomDuels configurations reloaded.");
                return true;
            }
            sender.sendMessage("This command is player-only.");
            return true;
        }

        if (args.length == 0) {
            new MainMenu(plugin, player).open();
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "menu" -> new MainMenu(plugin, player).open();

            case "duel" -> {
                if (args.length < 2) {
                    plugin.getMessageManager().sendRawMessage(player, "{prefix} &fИспользование: &e/duels duel <ник>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    plugin.getMessageManager().sendMessage(player, "player-not-found", Map.of("TARGET", args[1]));
                    return true;
                }
                new DuelSettingsMenu(plugin, player, target, new RuleSet()).open();
            }

            case "accept" -> {
                Player challenger = args.length > 1 ? Bukkit.getPlayer(args[1]) : null;
                plugin.getDuelManager().acceptChallenge(player, challenger != null ? challenger.getUniqueId() : null);
            }

            case "deny" -> plugin.getDuelManager().denyChallenge(player);

            case "queue" -> new QueueMenu(plugin, player).open();

            case "open" -> new OpenChallengesMenu(plugin, player).open();

            case "bot" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("duel")) {
                    new BotDifficultyMenu(plugin, player).open();
                } else {
                    new com.ejyqyl.atomduels.gui.BotSettingsMenu(plugin, player).open();
                }
            }

            case "stats" -> {
                Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : player;
                if (target == null) {
                    plugin.getMessageManager().sendMessage(player, "player-not-found", Map.of("TARGET", args[1]));
                    return true;
                }
                new StatsMenu(plugin, player, target.getUniqueId()).open();
            }

            case "top" -> {
                plugin.getMessageManager().sendRawMessage(player, "");
                plugin.getMessageManager().sendRawMessage(player, "  <gradient:#00B5FD:#7670E5>✦ ТОП-10 ИГРОКОВ ПО РЕЙТИНГУ (ELO) ✦</gradient>");
                List<PlayerData> top = plugin.getStatsManager().getTopByElo(10);
                int rank = 1;
                for (PlayerData d : top) {
                    String line = "  &e#" + rank + " &f" + d.getUsername() + " &8— &b" + d.getElo() + " Elo &8(" + d.getRank().getFormattedName() + "&8)";
                    plugin.getMessageManager().sendRawMessage(player, line);
                    rank++;
                }
                plugin.getMessageManager().sendRawMessage(player, "");
            }

            case "surrender", "forfeit" -> plugin.getDuelManager().handleSurrender(player);

            case "spectator", "spec" -> {
                if (args.length < 2) {
                    plugin.getMessageManager().sendRawMessage(player, "{prefix} &fИспользование: &e/duels spec <ник>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    plugin.getMessageManager().sendMessage(player, "player-not-found", Map.of("TARGET", args[1]));
                    return true;
                }
                ActiveDuel duel = plugin.getDuelManager().getDuel(target.getUniqueId());
                if (duel == null) {
                    plugin.getMessageManager().sendRawMessage(player, "{prefix} &cЭтот игрок не находится в дуэли!");
                    return true;
                }
                player.teleport(duel.getArena().getSpectatorSpawn());
                duel.addSpectator(player.getUniqueId());
                plugin.getMessageManager().sendRawMessage(player, "{prefix} &aВы наблюдаете за боем &e" + target.getName());
            }

            case "claim" -> new ClaimMenu(plugin, player).open();

            case "loot" -> new LootMenu(plugin, player).open();

            case "arena" -> {
                if (!player.hasPermission("atomduels.admin")) {
                    plugin.getMessageManager().sendMessage(player, "no-permission");
                    return true;
                }
                if (args.length == 1) {
                    new AdminArenaMenu(plugin, player).open();
                    return true;
                }
                handleArenaAdmin(player, args);
            }

            case "kit" -> {
                if (!player.hasPermission("atomduels.admin")) {
                    plugin.getMessageManager().sendMessage(player, "no-permission");
                    return true;
                }
                if (args.length == 1) {
                    new AdminKitMenu(plugin, player).open();
                    return true;
                }
                handleKitAdmin(player, args);
            }

            case "reload" -> {
                if (!player.hasPermission("atomduels.admin")) {
                    plugin.getMessageManager().sendMessage(player, "no-permission");
                    return true;
                }
                plugin.reloadAll();
                plugin.getMessageManager().sendRawMessage(player, "{prefix} &aКонфигурация и данные успешно перезагружены!");
            }

            case "help" -> sendHelp(player);

            default -> plugin.getMessageManager().sendMessage(player, "unknown-command");
        }
        return true;
    }

    private void handleArenaAdmin(Player player, String[] args) {
        String action = args[1].toLowerCase();
        switch (action) {
            case "create" -> {
                if (args.length < 3) {
                    plugin.getMessageManager().sendRawMessage(player, "{prefix} &c/duels arena create <имя>");
                    return;
                }
                Arena arena = plugin.getArenaManager().createArena(args[2]);
                plugin.getMessageManager().sendRawMessage(player, "{prefix} &aАрена &e" + arena.getName() + " &aсоздана!");
            }
            case "delete" -> {
                if (args.length < 3) {
                    plugin.getMessageManager().sendRawMessage(player, "{prefix} &c/duels arena delete <имя>");
                    return;
                }
                boolean deleted = plugin.getArenaManager().deleteArena(args[2]);
                if (deleted) plugin.getMessageManager().sendRawMessage(player, "{prefix} &aАрена &e" + args[2] + " &aудалена!");
                else plugin.getMessageManager().sendRawMessage(player, "{prefix} &cАрена не найдена.");
            }
            case "setspawn1" -> {
                if (args.length < 3) return;
                Arena a = plugin.getArenaManager().getArena(args[2]);
                if (a != null) {
                    a.setSpawn1(player.getLocation().clone());
                    plugin.getArenaManager().saveArenas();
                    plugin.getMessageManager().sendRawMessage(player, "{prefix} &aSpawn 1 установлен для " + a.getName());
                }
            }
            case "setspawn2" -> {
                if (args.length < 3) return;
                Arena a = plugin.getArenaManager().getArena(args[2]);
                if (a != null) {
                    a.setSpawn2(player.getLocation().clone());
                    plugin.getArenaManager().saveArenas();
                    plugin.getMessageManager().sendRawMessage(player, "{prefix} &aSpawn 2 установлен для " + a.getName());
                }
            }
            default -> new AdminArenaMenu(plugin, player).open();
        }
    }

    private void handleKitAdmin(Player player, String[] args) {
        String action = args[1].toLowerCase();
        switch (action) {
            case "create" -> {
                if (args.length < 3) {
                    plugin.getMessageManager().sendRawMessage(player, "{prefix} &c/duels kit create <имя>");
                    return;
                }
                plugin.getKitManager().createKitFromPlayer(args[2], player);
                plugin.getMessageManager().sendRawMessage(player, "{prefix} &aКит &e" + args[2] + " &aсохранён!");
            }
            case "delete" -> {
                if (args.length < 3) {
                    plugin.getMessageManager().sendRawMessage(player, "{prefix} &c/duels kit delete <имя>");
                    return;
                }
                plugin.getKitManager().deleteKit(args[2]);
                plugin.getMessageManager().sendRawMessage(player, "{prefix} &aКит &e" + args[2] + " &aудалён!");
            }
            case "give" -> {
                if (args.length < 3) return;
                Kit kit = plugin.getKitManager().getKit(args[2]);
                if (kit != null) {
                    kit.apply(player);
                    plugin.getMessageManager().sendRawMessage(player, "{prefix} &aКит &e" + kit.getName() + " &aвыдан!");
                }
            }
            default -> new AdminKitMenu(plugin, player).open();
        }
    }

    private void sendHelp(Player player) {
        plugin.getMessageManager().sendRawMessage(player, "");
        plugin.getMessageManager().sendRawMessage(player, "  <gradient:#00B5FD:#7670E5>✦ АТОМДУЭЛИ — КОМАНДЫ ✦</gradient>");
        plugin.getMessageManager().sendRawMessage(player, "  &b/duels &8— &7Открыть главное меню");
        plugin.getMessageManager().sendRawMessage(player, "  &b/duels duel <ник> &8— &7Бросить вызов игроку");
        plugin.getMessageManager().sendRawMessage(player, "  &b/duels accept [ник] &8— &7Принять вызов");
        plugin.getMessageManager().sendRawMessage(player, "  &b/duels deny [ник] &8— &7Отклонить вызов");
        plugin.getMessageManager().sendRawMessage(player, "  &b/duels queue &8— &7Поиск боя по рейтингу Elo");
        plugin.getMessageManager().sendRawMessage(player, "  &b/duels open &8— &7Список открытых вызовов");
        plugin.getMessageManager().sendRawMessage(player, "  &b/duels bot &8— &7Настройка и призыв тренировочного NPC-бота");
        plugin.getMessageManager().sendRawMessage(player, "  &b/duels stats [ник] &8— &7Статистика игрока");
        plugin.getMessageManager().sendRawMessage(player, "  &b/duels top &8— &7Топ игроков по рейтингу");
        plugin.getMessageManager().sendRawMessage(player, "  &b/duels claim &8— &7Забрать сохранённые вещи");
        plugin.getMessageManager().sendRawMessage(player, "  &b/duels loot &8— &7Трофеи с боев со своими вещами");
        plugin.getMessageManager().sendRawMessage(player, "  &b/duels surrender &8— &7Сдаться в текущем бою");
        plugin.getMessageManager().sendRawMessage(player, "");
        plugin.getMessageManager().sendRawMessage(player, "  &7Разработчик: &e[aqkooo (ejyqyl)](https://github.com/aqkooo)");
        plugin.getMessageManager().sendRawMessage(player, "");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String s : SUBCOMMANDS) {
                if (s.startsWith(args[0].toLowerCase())) list.add(s);
            }
            return list;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("duel") || sub.equals("stats") || sub.equals("spec") || sub.equals("spectator") || sub.equals("accept")) {
                List<String> list = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) list.add(p.getName());
                }
                return list;
            }
            if (sub.equals("arena")) {
                return Arrays.asList("create", "delete", "setspawn1", "setspawn2", "setspec");
            }
            if (sub.equals("kit")) {
                return Arrays.asList("create", "delete", "give");
            }
        }
        return Collections.emptyList();
    }
}
