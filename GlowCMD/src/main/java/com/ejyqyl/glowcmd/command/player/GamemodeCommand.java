package com.ejyqyl.glowcmd.command.player;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /gamemode <mode> [player], /gm, /gmc, /gms, /gma, /gmsp.
 *
 * @author ejyqyl
 */
public final class GamemodeCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;

    public GamemodeCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("glowcmd.gamemode")) {
            messageManager.send(sender, "no-permission");
            return true;
        }

        GameMode mode = null;
        String lowerLabel = label.toLowerCase();
        if (lowerLabel.equals("gmc")) {
            mode = GameMode.CREATIVE;
        } else if (lowerLabel.equals("gms")) {
            mode = GameMode.SURVIVAL;
        } else if (lowerLabel.equals("gma")) {
            mode = GameMode.ADVENTURE;
        } else if (lowerLabel.equals("gmsp")) {
            mode = GameMode.SPECTATOR;
        }

        Player target;
        if (mode != null) {
            // Shorthand command (/gmc [player])
            if (args.length > 0) {
                if (!sender.hasPermission("glowcmd.gamemode.others")) {
                    messageManager.send(sender, "no-permission");
                    return true;
                }
                target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    messageManager.send(sender, "player-not-found", Map.of("{TARGET}", args[0]));
                    return true;
                }
            } else {
                if (!(sender instanceof Player p)) {
                    messageManager.send(sender, "player-only");
                    return true;
                }
                target = p;
            }
        } else {
            // Regular /gamemode <mode> [player]
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /" + label + " <survival|creative|adventure|spectator> [player]");
                return true;
            }

            mode = parseGameMode(args[0]);
            if (mode == null) {
                messageManager.send(sender, "gamemode.invalid", Map.of("{MODE}", args[0]));
                return true;
            }

            if (args.length > 1) {
                if (!sender.hasPermission("glowcmd.gamemode.others")) {
                    messageManager.send(sender, "no-permission");
                    return true;
                }
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    messageManager.send(sender, "player-not-found", Map.of("{TARGET}", args[1]));
                    return true;
                }
            } else {
                if (!(sender instanceof Player p)) {
                    messageManager.send(sender, "player-only");
                    return true;
                }
                target = p;
            }
        }

        target.setGameMode(mode);
        String modeName = mode.name().toLowerCase();
        if (target.equals(sender)) {
            messageManager.send(target, "gamemode.changed", Map.of("{MODE}", modeName));
        } else {
            messageManager.send(sender, "gamemode.changed-other", Map.of("{MODE}", modeName, "{TARGET}", target.getName()));
            messageManager.send(target, "gamemode.changed", Map.of("{MODE}", modeName));
        }
        return true;
    }

    @Nullable
    private GameMode parseGameMode(@NotNull String input) {
        return switch (input.toLowerCase()) {
            case "0", "s", "survival" -> GameMode.SURVIVAL;
            case "1", "c", "creative" -> GameMode.CREATIVE;
            case "2", "a", "adventure" -> GameMode.ADVENTURE;
            case "3", "sp", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && (alias.equalsIgnoreCase("gamemode") || alias.equalsIgnoreCase("gm"))) {
            List<String> list = new ArrayList<>();
            for (String mode : List.of("survival", "creative", "adventure", "spectator")) {
                if (mode.startsWith(args[0].toLowerCase())) list.add(mode);
            }
            return list;
        }
        return Collections.emptyList();
    }
}
