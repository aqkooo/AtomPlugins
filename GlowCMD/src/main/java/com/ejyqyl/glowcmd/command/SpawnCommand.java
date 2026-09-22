package com.ejyqyl.glowcmd.command;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.ConfigManager;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.model.SpawnPoint;
import com.ejyqyl.glowcmd.spawn.SpawnManager;
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
 * Command handler for /spawn.
 * Teleports the calling player to the configured spawn point.
 *
 * @author ejyqyl
 */
public final class SpawnCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final SpawnManager spawnManager;

    public SpawnCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.spawnManager = plugin.getSpawnManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 1. Console check
        if (!(sender instanceof Player)) {
            messageManager.send(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;

        // 2. Permission check
        if (!player.hasPermission("glowcmd.spawn")) {
            messageManager.send(player, "no-permission");
            return true;
        }

        // 3. System enabled check
        if (!configManager.isSpawnEnabled()) {
            messageManager.send(player, "spawn.not-set");
            return true;
        }

        // 4. Check if spawn is configured
        SpawnPoint point = spawnManager.getSpawn();
        if (point == null) {
            messageManager.send(player, "spawn.not-set");
            return true;
        }

        // 5. Check if target world exists
        Location location = point.toLocation();
        if (location == null) {
            messageManager.send(player, "spawn.world-not-found");
            return true;
        }

        // 6. Execute teleportation
        player.teleport(location);
        messageManager.send(player, "spawn.teleport");
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
