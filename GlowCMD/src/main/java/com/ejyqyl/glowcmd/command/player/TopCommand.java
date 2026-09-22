package com.ejyqyl.glowcmd.command.player;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
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
 * /top command. Teleports player to the highest block above them.
 *
 * @author ejyqyl
 */
public final class TopCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;

    public TopCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.send(sender, "player-only");
            return true;
        }

        if (!player.hasPermission("glowcmd.top")) {
            messageManager.send(player, "no-permission");
            return true;
        }

        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return true;

        Block highest = world.getHighestBlockAt(loc);
        Location topLoc = highest.getLocation().add(0.5, 1.0, 0.5);
        topLoc.setYaw(loc.getYaw());
        topLoc.setPitch(loc.getPitch());

        player.teleport(topLoc);
        messageManager.send(player, "top.teleported");
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
