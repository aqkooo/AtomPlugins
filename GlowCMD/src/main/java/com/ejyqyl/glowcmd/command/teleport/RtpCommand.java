package com.ejyqyl.glowcmd.command.teleport;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.manager.TeleportManager;
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
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * /rtp command (CMI feature). Teleports the player to a safe random location in the world.
 *
 * @author ejyqyl
 */
public final class RtpCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final TeleportManager teleportManager;

    public RtpCommand(@NotNull GlowCMD plugin) {
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

        if (!player.hasPermission("glowcmd.rtp")) {
            messageManager.send(player, "no-permission");
            return true;
        }

        World world = player.getWorld();
        if (world.getEnvironment() == World.Environment.NETHER || world.getEnvironment() == World.Environment.THE_END) {
            player.sendMessage("§cRTP is only available in the Overworld.");
            return true;
        }

        Location safeLoc = findSafeLocation(world, 250, 2000);
        if (safeLoc == null) {
            player.sendMessage("§cFailed to find a safe location. Please try again.");
            return true;
        }

        teleportManager.setLastLocation(player.getUniqueId(), player.getLocation());
        player.teleport(safeLoc);

        messageManager.send(player, "rtp.success", Map.of(
                "{X}", String.valueOf(safeLoc.getBlockX()),
                "{Y}", String.valueOf(safeLoc.getBlockY()),
                "{Z}", String.valueOf(safeLoc.getBlockZ())
        ));
        return true;
    }

    @Nullable
    private Location findSafeLocation(@NotNull World world, int minRadius, int maxRadius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempts = 0; attempts < 15; attempts++) {
            int x = random.nextInt(minRadius, maxRadius);
            if (random.nextBoolean()) x = -x;
            int z = random.nextInt(minRadius, maxRadius);
            if (random.nextBoolean()) z = -z;

            Block highest = world.getHighestBlockAt(x, z);
            if (!highest.isLiquid() && highest.getType().isSolid()) {
                Block above1 = highest.getRelative(0, 1, 0);
                Block above2 = highest.getRelative(0, 2, 0);
                if (above1.isEmpty() && above2.isEmpty()) {
                    return highest.getLocation().add(0.5, 1.0, 0.5);
                }
            }
        }
        return null;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
