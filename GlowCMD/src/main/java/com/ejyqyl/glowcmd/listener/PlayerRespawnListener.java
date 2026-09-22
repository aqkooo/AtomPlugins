package com.ejyqyl.glowcmd.listener;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.ConfigManager;
import com.ejyqyl.glowcmd.model.SpawnPoint;
import com.ejyqyl.glowcmd.spawn.SpawnManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Handles player respawn behavior upon death according to configuration.
 * If teleport-on-death is disabled, vanilla respawn behavior is preserved.
 *
 * @author ejyqyl
 */
public final class PlayerRespawnListener implements Listener {

    private final GlowCMD plugin;
    private final ConfigManager configManager;
    private final SpawnManager spawnManager;

    public PlayerRespawnListener(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.spawnManager = plugin.getSpawnManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // 1. Check if death respawn redirection is enabled
        if (!configManager.isSpawnEnabled() || !configManager.isTeleportOnDeath()) {
            return;
        }

        // 2. Check if bed or respawn anchor should be respected
        if (configManager.isRespectBedSpawn()) {
            boolean hasBedOrAnchor = event.isBedSpawn();
            try {
                hasBedOrAnchor = hasBedOrAnchor || event.isAnchorSpawn();
            } catch (Throwable ignored) {
            }
            if (hasBedOrAnchor) {
                return;
            }
        }

        // 3. Check if spawn point is configured
        SpawnPoint spawn = spawnManager.getSpawn();
        if (spawn == null) {
            return;
        }

        // 3. Resolve location safely
        Location location = spawn.toLocation();
        if (location != null && location.getWorld() != null) {
            event.setRespawnLocation(location);
        }
    }
}
