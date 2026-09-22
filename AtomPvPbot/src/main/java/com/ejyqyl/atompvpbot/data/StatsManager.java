package com.ejyqyl.atompvpbot.data;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory caching and management for player statistics.
 *
 * @author ejyqyl
 */
public class StatsManager {

    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public StatsManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId(), player.getName());
    }

    public PlayerData getPlayerData(UUID uuid, String username) {
        return cache.computeIfAbsent(uuid, id -> databaseManager.loadPlayerData(id, username != null ? username : "Player"));
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            databaseManager.savePlayerData(data);
        }
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            databaseManager.savePlayerData(data);
        }
    }

    public void unloadPlayer(UUID uuid) {
        savePlayerData(uuid);
        cache.remove(uuid);
    }
}
