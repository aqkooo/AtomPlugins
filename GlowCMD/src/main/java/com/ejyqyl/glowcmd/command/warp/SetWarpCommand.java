package com.ejyqyl.glowcmd.command.warp;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.manager.WarpManager;
import com.ejyqyl.glowcmd.model.SpawnPoint;
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
 * /setwarp <name>.
 *
 * @author ejyqyl
 */
public final class SetWarpCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final WarpManager warpManager;

    public SetWarpCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.warpManager = plugin.getWarpManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.send(sender, "player-only");
            return true;
        }

        if (!player.hasPermission("glowcmd.setwarp")) {
            messageManager.send(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /setwarp <name>");
            return true;
        }

        String warpName = args[0].toLowerCase();
        SpawnPoint point = SpawnPoint.fromLocation(player.getLocation());
        warpManager.setWarp(warpName, point);

        messageManager.send(player, "warp.set", Map.of("{WARP}", warpName));
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
