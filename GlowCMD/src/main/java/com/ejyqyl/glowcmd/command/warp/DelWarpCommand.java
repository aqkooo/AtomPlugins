package com.ejyqyl.glowcmd.command.warp;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.manager.WarpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /delwarp <name>.
 *
 * @author ejyqyl
 */
public final class DelWarpCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final WarpManager warpManager;

    public DelWarpCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.warpManager = plugin.getWarpManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("glowcmd.delwarp")) {
            messageManager.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /delwarp <name>");
            return true;
        }

        String warpName = args[0].toLowerCase();
        if (warpManager.deleteWarp(warpName)) {
            messageManager.send(sender, "warp.deleted", Map.of("{WARP}", warpName));
        } else {
            messageManager.send(sender, "warp.not-found", Map.of("{WARP}", warpName));
        }
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
