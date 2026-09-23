package glowseller.cache;

import glowseller.models.PlayerData;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerDataCache {
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();

    public PlayerData get(UUID uuid) {
        if (uuid == null) return null;
        return cache.get(uuid);
    }

    public PlayerData getOrCreate(UUID uuid) {
        if (uuid == null) return null;
        return cache.computeIfAbsent(uuid, PlayerData::new);
    }

    public void put(UUID uuid, PlayerData data) {
        if (uuid != null && data != null) {
            cache.put(uuid, data);
            loading.remove(uuid);
        }
    }

    public void mergeAndPut(UUID uuid, PlayerData loaded) {
        if (uuid == null || loaded == null) return;
        cache.compute(uuid, (k, existing) -> {
            if (existing != null && existing.isDirty()) {
                loaded.addPoints(existing.getPoints());
                if (existing.hasActiveBooster()) {
                    loaded.setActiveBoosterKey(existing.getActiveBoosterKey());
                    loaded.setBoosterExpireAt(existing.getBoosterExpireAt());
                }
            }
            return loaded;
        });
        loading.remove(uuid);
    }

    public void markLoading(UUID uuid) {
        if (uuid != null) loading.add(uuid);
    }

    public void unmarkLoading(UUID uuid) {
        if (uuid != null) loading.remove(uuid);
    }

    public boolean isLoading(UUID uuid) {
        return uuid != null && loading.contains(uuid);
    }

    public PlayerData remove(UUID uuid) {
        if (uuid == null) return null;
        loading.remove(uuid);
        return cache.remove(uuid);
    }

    public Collection<PlayerData> getAll() {
        return cache.values();
    }

    public Collection<PlayerData> getDirtyData() {
        return cache.values().stream()
                .filter(PlayerData::isDirty)
                .collect(Collectors.toList());
    }

    public void clear() {
        cache.clear();
        loading.clear();
    }
}
