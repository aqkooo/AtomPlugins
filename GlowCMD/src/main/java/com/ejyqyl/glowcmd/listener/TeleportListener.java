package com.ejyqyl.glowcmd.listener;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.manager.PrivateMessageManager;
import com.ejyqyl.glowcmd.manager.TeleportManager;
import com.ejyqyl.glowcmd.manager.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listens to teleport, death, join and quit events to maintain
 * /back history, vanish visibility, and TPA cleanup.
 *
 * @author ejyqyl
 */
public final class TeleportListener implements Listener {

    private final GlowCMD plugin;
    private final TeleportManager teleportManager;
    private final VanishManager vanishManager;
    private final PrivateMessageManager pmManager;

    public TeleportListener(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.teleportManager = plugin.getTeleportManager();
        this.vanishManager = plugin.getVanishManager();
        this.pmManager = plugin.getPrivateMessageManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Track previous location for /back
        teleportManager.setLastLocation(event.getPlayer().getUniqueId(), event.getFrom());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        teleportManager.setLastLocation(player.getUniqueId(), player.getLocation());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        vanishManager.onPlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        teleportManager.removeAllForPlayer(player.getUniqueId());
        vanishManager.onPlayerQuit(player);
        pmManager.removePlayer(player.getUniqueId());
    }
}
