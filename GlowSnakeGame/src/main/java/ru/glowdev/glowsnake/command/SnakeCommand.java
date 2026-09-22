package ru.glowdev.glowsnake.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.glowdev.glowsnake.GlowSnakePlugin;
import ru.glowdev.glowsnake.config.ConfigManager;
import ru.glowdev.glowsnake.storage.ScoreManager;
import ru.glowdev.glowsnake.util.ChatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SnakeCommand implements CommandExecutor, TabCompleter {

    public static final String PERM_PLAY = "glowsnake.play";
    public static final String PERM_ADMIN = "glowsnake.admin";

    private final GlowSnakePlugin plugin;

    public SnakeCommand(GlowSnakePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager cfg = plugin.getConfigManager();

        if (args.length > 0) {
            String sub = args[0].toLowerCase();

            if (sub.equals("reload")) {
                if (!sender.hasPermission(PERM_ADMIN)) {
                    sender.sendMessage(cfg.getPrefixedMessage("no-permission", "&cУ вас нет прав для выполнения этой команды!"));
                    return true;
                }

                cfg.load();
                plugin.getScoreManager().load();
                sender.sendMessage(cfg.getPrefixedMessage("reload-success", "&aКонфигурация успешно перезагружена!"));
                return true;
            }

            if (sub.equals("top")) {
                List<ScoreManager.ScoreEntry> topScores = plugin.getScoreManager().getTopScores(10);
                sender.sendMessage(ChatUtil.color(cfg.getMessage("top-header", "&6=== &eТоп игроков GlowSnake &6===")));

                if (topScores.isEmpty()) {
                    sender.sendMessage(ChatUtil.color(cfg.getMessage("top-empty", "&7Таблица рекордов пока пуста.")));
                } else {
                    int pos = 1;
                    for (ScoreManager.ScoreEntry entry : topScores) {
                        String line = cfg.getMessage("top-entry", "&e{POS}. &f{PLAYER} &7— &a{SCORE} &7очков")
                                .replace("{POS}", String.valueOf(pos))
                                .replace("{PLAYER}", entry.getName())
                                .replace("{SCORE}", String.valueOf(entry.getHighScore()));
                        sender.sendMessage(ChatUtil.color(line));
                        pos++;
                    }
                }
                return true;
            }

            if (sub.equals("help")) {
                sender.sendMessage(ChatUtil.color("&2=== &aGlowSnakeGame &2==="));
                sender.sendMessage(ChatUtil.color("&a/" + label + " &7— Запустить игру «Змейка»"));
                sender.sendMessage(ChatUtil.color("&a/" + label + " top &7— Просмотреть таблицу рекордов"));
                if (sender.hasPermission(PERM_ADMIN)) {
                    sender.sendMessage(ChatUtil.color("&a/" + label + " reload &7— Перезагрузить конфигурацию"));
                }
                return true;
            }
        }

        // Open game for player
        if (!(sender instanceof Player)) {
            sender.sendMessage(cfg.getPrefixedMessage("only-players", "&cЭту команду могут использовать только игроки!"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission(PERM_PLAY)) {
            player.sendMessage(cfg.getPrefixedMessage("no-permission", "&cУ вас нет прав для выполнения этой команды!"));
            return true;
        }

        plugin.getGameManager().startGame(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission(PERM_ADMIN)) {
                completions.add("reload");
            }
            completions.add("top");
            completions.add("help");

            String input = args[0].toLowerCase();
            List<String> matched = new ArrayList<>();
            for (String c : completions) {
                if (c.startsWith(input)) {
                    matched.add(c);
                }
            }
            return matched;
        }
        return Collections.emptyList();
    }
}
