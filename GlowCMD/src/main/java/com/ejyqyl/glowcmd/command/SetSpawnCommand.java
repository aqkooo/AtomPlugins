package com.ejyqyl.glowcmd.command;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.model.SpawnPoint;
import com.ejyqyl.glowcmd.spawn.SpawnManager;
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
 * Command handler for /setspawn.
 * Sets the global server spawn point to the player's current location.
 *
 * @author ejyqyl
 */
public final class SetSpawnCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final SpawnManager spawnManager;

    public SetSpawnCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
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
        if (!player.hasPermission("glowcmd.setspawn")) {
            messageManager.send(player, "no-permission");
            return true;
        }

        // 3. Save spawn location
        SpawnPoint point = SpawnPoint.fromLocation(player.getLocation());
        spawnManager.setSpawn(point);

        // 4. Send success message
        messageManager.send(player, "spawn.set");
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
