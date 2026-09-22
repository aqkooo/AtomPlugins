package com.ejyqyl.glowcmd.command.player;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /feed [player] command. Restores food and saturation.
 *
 * @author ejyqyl
 */
public final class FeedCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;

    public FeedCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                messageManager.send(sender, "player-only");
                return true;
            }
            if (!player.hasPermission("glowcmd.feed")) {
                messageManager.send(player, "no-permission");
                return true;
            }

            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            messageManager.send(player, "feed.self");
            return true;
        }

        // Feed another player
        if (!sender.hasPermission("glowcmd.feed.others")) {
            messageManager.send(sender, "no-permission");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            messageManager.send(sender, "player-not-found", Map.of("{TARGET}", args[0]));
            return true;
        }

        target.setFoodLevel(20);
        target.setSaturation(20.0f);
        messageManager.send(sender, "feed.other", Map.of("{TARGET}", target.getName()));
        messageManager.send(target, "feed.by-other", Map.of("{SENDER}", sender.getName()));
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("glowcmd.feed.others")) {
            return null; // Bukkit default player name completion
        }
        return Collections.emptyList();
    }
}
