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
 * /tp <target> or /tp <player> <target>.
 *
 * @author ejyqyl
 */
public final class TeleportCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final TeleportManager teleportManager;

    public TeleportCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.teleportManager = plugin.getTeleportManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /tp <player> [target]");
            return true;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                messageManager.send(sender, "player-only");
                return true;
            }
            if (!player.hasPermission("glowcmd.tp")) {
                messageManager.send(player, "no-permission");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                messageManager.send(player, "player-not-found", Map.of("{TARGET}", args[0]));
                return true;
            }

            teleportManager.setLastLocation(player.getUniqueId(), player.getLocation());
            player.teleport(target.getLocation());
            messageManager.send(player, "tp.success", Map.of("{TARGET}", target.getName()));
            return true;
        }

        // /tp <player> <target>
        if (!sender.hasPermission("glowcmd.tp.others")) {
            messageManager.send(sender, "no-permission");
            return true;
        }

        Player playerToTeleport = Bukkit.getPlayer(args[0]);
        Player target = Bukkit.getPlayer(args[1]);

        if (playerToTeleport == null) {
            messageManager.send(sender, "player-not-found", Map.of("{TARGET}", args[0]));
            return true;
        }
        if (target == null) {
            messageManager.send(sender, "player-not-found", Map.of("{TARGET}", args[1]));
            return true;
        }

        teleportManager.setLastLocation(playerToTeleport.getUniqueId(), playerToTeleport.getLocation());
        playerToTeleport.teleport(target.getLocation());

        messageManager.send(sender, "tp.other-success", Map.of("{PLAYER}", playerToTeleport.getName(), "{TARGET}", target.getName()));
        messageManager.send(playerToTeleport, "tp.success", Map.of("{TARGET}", target.getName()));
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if ((args.length == 1 && sender.hasPermission("glowcmd.tp")) ||
            (args.length == 2 && sender.hasPermission("glowcmd.tp.others"))) {
            return null;
        }
        return Collections.emptyList();
    }
}
