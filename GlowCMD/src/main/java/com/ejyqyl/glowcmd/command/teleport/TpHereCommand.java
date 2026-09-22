package com.ejyqyl.glowcmd.command.teleport;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.manager.TeleportManager;
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
 * /tphere <player>. Teleports target player to sender's location.
 *
 * @author ejyqyl
 */
public final class TpHereCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final TeleportManager teleportManager;

    public TpHereCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.teleportManager = plugin.getTeleportManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.send(sender, "player-only");
            return true;
        }

        if (!player.hasPermission("glowcmd.tphere")) {
            messageManager.send(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /tphere <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            messageManager.send(player, "player-not-found", Map.of("{TARGET}", args[0]));
            return true;
        }

        teleportManager.setLastLocation(target.getUniqueId(), target.getLocation());
        target.teleport(player.getLocation());

        messageManager.send(player, "tp.tphere-success", Map.of("{TARGET}", target.getName()));
        messageManager.send(target, "tp.teleported-to-sender", Map.of("{SENDER}", player.getName()));
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("glowcmd.tphere")) {
            return null;
        }
        return Collections.emptyList();
    }
}
