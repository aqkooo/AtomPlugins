package com.ejyqyl.glowcmd.command.warp;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.manager.WarpManager;
import com.ejyqyl.glowcmd.model.SpawnPoint;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /warps. Lists all available server warps.
 *
 * @author ejyqyl
 */
public final class WarpsCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final WarpManager warpManager;

    public WarpsCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.warpManager = plugin.getWarpManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("glowcmd.warps")) {
            messageManager.send(sender, "no-permission");
            return true;
        }

        Map<String, SpawnPoint> warps = warpManager.getWarps();
        if (warps.isEmpty()) {
            messageManager.send(sender, "warp.none");
            return true;
        }

        String list = String.join(", ", warps.keySet());
        messageManager.send(sender, "warp.list", Map.of("{WARPS}", list, "{COUNT}", String.valueOf(warps.size())));
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
