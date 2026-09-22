package com.ejyqyl.glowchatgame.command;

import com.ejyqyl.glowchatgame.GlowChatGame;
import com.ejyqyl.glowchatgame.config.MessageManager;
import com.ejyqyl.glowchatgame.data.PlayerStats;
import com.ejyqyl.glowchatgame.game.Difficulty;
import com.ejyqyl.glowchatgame.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Command executor and tab completer for /chatgame.
 *
 * @author ejyqyl, Glowdevv
 */
public final class ChatGameCommand implements CommandExecutor, TabCompleter {

    private final GlowChatGame plugin;
    private final MessageManager messageManager;
    private final GameManager gameManager;

    public ChatGameCommand(@NotNull GlowChatGame plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.gameManager = plugin.getGameManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            messageManager.send(sender, "help");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "start":
                handleStart(sender, args);
                return true;

            case "stop":
                handleStop(sender);
                return true;

            case "reload":
                handleReload(sender);
                return true;

            case "stats":
                handleStats(sender, args);
                return true;

            default:
                messageManager.send(sender, "unknown-command");
                return true;
        }
    }

    private void handleStart(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("glowchatgame.admin")) {
            messageManager.send(sender, "no-permission");
            return;
        }

        if (gameManager.isGameRunning()) {
            messageManager.send(sender, "game.already-running");
            return;
        }

        Difficulty diff = null;
        if (args.length >= 2) {
            diff = Difficulty.fromString(args[1], Difficulty.MEDIUM);
        }

        boolean started = gameManager.startNewGame(diff);
        if (started) {
            String diffName = diff != null ? diff.getDisplayName() : "Случайный";
            messageManager.send(sender, "game.force-started", Map.of("{DIFFICULTY}", diffName));
        } else {
            messageManager.send(sender, "game.already-running");
        }
    }

    private void handleStop(@NotNull CommandSender sender) {
        if (!sender.hasPermission("glowchatgame.admin")) {
            messageManager.send(sender, "no-permission");
            return;
        }

        if (!gameManager.isGameRunning()) {
            messageManager.send(sender, "game.no-game");
            return;
        }

        gameManager.stopCurrentGame();
    }

    private void handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("glowchatgame.admin")) {
            messageManager.send(sender, "no-permission");
            return;
        }

        plugin.reloadAll();
        messageManager.send(sender, "game.reloaded");
    }

    private void handleStats(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length <= 1) {
            if (!(sender instanceof Player player)) {
                messageManager.send(sender, "player-only");
                return;
            }

            displayStats(player, plugin.getStatsManager().getOrCreateStats(player));
            return;
        }

        // Stats for another player
        if (!sender.hasPermission("glowchatgame.admin")) {
            messageManager.send(sender, "no-permission");
            return;
        }

        String targetName = args[1];
        plugin.getStatsManager().getStatsByName(targetName).thenAccept(stats -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (stats == null) {
                    messageManager.send(sender, "player-not-found", Map.of("{TARGET}", targetName));
                } else {
                    displayStats(sender, stats);
                }
            });
        });
    }

    private void displayStats(@NotNull CommandSender recipient, @NotNull PlayerStats stats) {
        Map<String, String> map = Map.of(
                "{PLAYER}", stats.getPlayerName(),
                "{WINS}", String.valueOf(stats.getWins()),
                "{GAMES}", String.valueOf(stats.getGames()),
                "{ACCURACY}", stats.getFormattedAccuracy(),
                "{REWARDS}", String.valueOf(stats.getRewardsClaimed())
        );
        messageManager.send(recipient, "stats.card", map);
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>(List.of("help", "stats"));
            if (sender.hasPermission("glowchatgame.admin")) {
                subcommands.add("start");
                subcommands.add("stop");
                subcommands.add("reload");
            }
            return filterMatching(subcommands, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("start") && sender.hasPermission("glowchatgame.admin")) {
            return filterMatching(List.of("easy", "medium", "hard"), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("stats") && sender.hasPermission("glowchatgame.admin")) {
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerNames.add(p.getName());
            }
            return filterMatching(playerNames, args[1]);
        }

        return Collections.emptyList();
    }

    private List<String> filterMatching(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(opt);
            }
        }
        return result;
    }
}
