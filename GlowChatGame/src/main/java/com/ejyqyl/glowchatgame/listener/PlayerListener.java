package com.ejyqyl.glowchatgame.listener;

import com.ejyqyl.glowchatgame.GlowChatGame;
import com.ejyqyl.glowchatgame.data.StatsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Handles caching and flushing of player statistics on connection events.
 *
 * @author ejyqyl, Glowdevv
 */
public final class PlayerListener implements Listener {

    private final GlowChatGame plugin;
    private final StatsManager statsManager;

    public PlayerListener(@NotNull GlowChatGame plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getStatsManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.getConfigManager().isStatsEnabled()) {
            statsManager.loadPlayer(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getConfigManager().isStatsEnabled()) {
            statsManager.unloadPlayer(event.getPlayer().getUniqueId());
        }
    }
}
