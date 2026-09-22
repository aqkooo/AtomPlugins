package com.ejyqyl.glowcmd.command.warp;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.manager.TeleportManager;
import com.ejyqyl.glowcmd.manager.WarpManager;
import com.ejyqyl.glowcmd.model.SpawnPoint;
import org.bukkit.Location;
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
 * /warp <name>.
 *
 * @author ejyqyl
 */
public final class WarpCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final WarpManager warpManager;
    private final TeleportManager teleportManager;

    public WarpCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.warpManager = plugin.getWarpManager();
        this.teleportManager = plugin.getTeleportManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.send(sender, "player-only");
            return true;
        }

        if (!player.hasPermission("glowcmd.warp")) {
            messageManager.send(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /warp <name>");
            return true;
        }

        String warpName = args[0].toLowerCase();
        SpawnPoint warp = warpManager.getWarp(warpName);

        if (warp == null) {
            messageManager.send(player, "warp.not-found", Map.of("{WARP}", warpName));
            return true;
        }

        Location loc = warp.toLocation();
        if (loc == null) {
            messageManager.send(player, "spawn.world-not-found");
            return true;
        }

        teleportManager.setLastLocation(player.getUniqueId(), player.getLocation());
        player.teleport(loc);
        messageManager.send(player, "warp.teleport", Map.of("{WARP}", warpName));
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String name : warpManager.getWarps().keySet()) {
                if (name.startsWith(args[0].toLowerCase())) list.add(name);
            }
            return list;
        }
        return Collections.emptyList();
    }
}
