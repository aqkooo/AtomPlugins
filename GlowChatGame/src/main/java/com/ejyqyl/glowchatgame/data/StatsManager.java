package com.ejyqyl.glowchatgame.data;

import com.ejyqyl.glowchatgame.GlowChatGame;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player statistics in memory and orchestrates asynchronous SQLite synchronization.
 *
 * @author ejyqyl, Glowdevv
 */
public final class StatsManager {

    private final GlowChatGame plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();

    public StatsManager(@NotNull GlowChatGame plugin, @NotNull DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void loadPlayer(@NotNull Player player) {
        databaseManager.loadStats(player.getUniqueId(), player.getName()).thenAccept(stats -> {
            stats.setPlayerName(player.getName());
            cache.put(player.getUniqueId(), stats);
        });
    }

    public void unloadPlayer(@NotNull UUID uuid) {
        PlayerStats stats = cache.remove(uuid);
        if (stats != null) {
            databaseManager.saveStats(stats);
        }
    }

    @NotNull
    public PlayerStats getOrCreateStats(@NotNull Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), u -> new PlayerStats(u, player.getName()));
    }

    @Nullable
    public PlayerStats getCachedStats(@NotNull UUID uuid) {
        return cache.get(uuid);
    }

    @NotNull
    public CompletableFuture<PlayerStats> getStatsByName(@NotNull String name) {
        for (PlayerStats s : cache.values()) {
            if (s.getPlayerName().equalsIgnoreCase(name)) {
                return CompletableFuture.completedFuture(s);
            }
        }
        return databaseManager.loadStatsByName(name);
    }

    public void recordWin(@NotNull Player player) {
        if (!plugin.getConfigManager().isStatsEnabled()) {
            return;
        }

        PlayerStats stats = getOrCreateStats(player);
        stats.incrementWins();
        stats.incrementGames();
        stats.incrementRewardsClaimed();
        stats.setLastWinTimestamp(System.currentTimeMillis());

        databaseManager.saveStats(stats);
    }

    public void recordGameParticipation(@NotNull Player player) {
        if (!plugin.getConfigManager().isStatsEnabled()) {
            return;
        }

        PlayerStats stats = getOrCreateStats(player);
        stats.incrementGames();
        databaseManager.saveStats(stats);
    }

    public void saveAll() {
        for (PlayerStats stats : cache.values()) {
            databaseManager.saveStats(stats);
        }
    }
}
