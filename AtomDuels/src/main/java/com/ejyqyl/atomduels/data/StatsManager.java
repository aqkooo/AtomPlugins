package com.ejyqyl.atomduels.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory statistics cache and persistence coordinator.
 * Authored by ejyqyl.
 */
public class StatsManager {

    private final DatabaseManager dbManager;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public StatsManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId(), player.getName());
    }

    public PlayerData getPlayerData(UUID uuid, String usernameFallback) {
        return cache.computeIfAbsent(uuid, id -> dbManager.loadPlayerData(id, usernameFallback));
    }

    public PlayerData getCachedData(UUID uuid) {
        return cache.get(uuid);
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            dbManager.runAsync(() -> dbManager.savePlayerData(data));
        }
    }

    public void savePlayerDataSync(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            dbManager.savePlayerData(data);
        }
    }

    public void unloadPlayer(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            dbManager.savePlayerData(data);
        }
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            dbManager.savePlayerData(data);
        }
    }

    public List<PlayerData> getTopByElo(int limit) {
        return dbManager.getTopByElo(limit);
    }

    public List<PlayerData> getTopByWins(int limit) {
        return dbManager.getTopByWins(limit);
    }
}
