package com.ejyqyl.glowcmd.listener;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.ConfigManager;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.model.SpawnPoint;
import com.ejyqyl.glowcmd.spawn.SpawnManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Handles first-join spawn teleportation for new players.
 * Subsequent logins by returning players are strictly ignored.
 *
 * @author ejyqyl
 */
public final class PlayerJoinListener implements Listener {

    private final GlowCMD plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final SpawnManager spawnManager;

    public PlayerJoinListener(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.spawnManager = plugin.getSpawnManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. Strict first-join check: do not teleport returning players
        if (player.hasPlayedBefore()) {
            return;
        }

        // 2. Check configuration toggles
        if (!configManager.isSpawnEnabled() ||
                !configManager.isFirstSpawnEnabled() ||
                !configManager.isTeleportOnFirstJoin()) {
            return;
        }

        // 3. Check if first spawn is configured
        SpawnPoint firstSpawn = spawnManager.getFirstSpawn();
        if (firstSpawn == null) {
            // Standard vanilla / server behavior if first-spawn is not set
            return;
        }

        // 4. Resolve world location safely
        Location location = firstSpawn.toLocation();
        if (location == null) {
            plugin.getLogger().warning("Failed to teleport new player " + player.getName() +
                    " to first-spawn: world '" + firstSpawn.getWorldName() + "' is not loaded.");
            return;
        }

        // 5. Teleport player to first spawn and deliver notification
        player.teleport(location);
        messageManager.send(player, "first-spawn.teleport");
    }
}
