package com.ejyqyl.glowcmd.command.admin;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * /seen <player>.
 *
 * @author ejyqyl
 */
public final class SeenCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public SeenCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("glowcmd.seen")) {
            messageManager.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /seen <player>");
            return true;
        }

        Player online = Bukkit.getPlayer(args[0]);
        if (online != null && online.isOnline()) {
            Location loc = online.getLocation();
            String world = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
            sender.sendMessage("§8=== §bSeen: §e" + online.getName() + " §8===");
            sender.sendMessage("§7Status: §aOnline");
            sender.sendMessage("§7World: §f" + world);
            sender.sendMessage("§7Location: §f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            if (sender.hasPermission("glowcmd.seen.ip") && online.getAddress() != null) {
                sender.sendMessage("§7IP: §f" + online.getAddress().getAddress().getHostAddress());
            }
            return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
        if (offline.hasPlayedBefore()) {
            sender.sendMessage("§8=== §bSeen: §e" + offline.getName() + " §8===");
            sender.sendMessage("§7Status: §cOffline");
            sender.sendMessage("§7Last seen: §f" + dateFormat.format(new Date(offline.getLastPlayed())));
            sender.sendMessage("§7First joined: §f" + dateFormat.format(new Date(offline.getFirstPlayed())));
        } else {
            sender.sendMessage("§cPlayer §e" + args[0] + " §chas never played on this server.");
        }
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return null;
        }
        return Collections.emptyList();
    }
}
