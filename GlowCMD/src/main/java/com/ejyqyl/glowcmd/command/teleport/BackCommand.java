package com.ejyqyl.glowcmd.command.teleport;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.manager.TeleportManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * /back command. Returns the player to their previous location (before teleport or death).
 *
 * @author ejyqyl
 */
public final class BackCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final TeleportManager teleportManager;

    public BackCommand(@NotNull GlowCMD plugin) {
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

        if (!player.hasPermission("glowcmd.back")) {
            messageManager.send(player, "no-permission");
            return true;
        }

        Location lastLoc = teleportManager.getLastLocation(player.getUniqueId());
        if (lastLoc == null) {
            messageManager.send(player, "back.no-location");
            return true;
        }

        if (lastLoc.getWorld() == null) {
            messageManager.send(player, "spawn.world-not-found");
            return true;
        }

        Location current = player.getLocation();
        player.teleport(lastLoc);
        teleportManager.setLastLocation(player.getUniqueId(), current);

        messageManager.send(player, "back.success");
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
