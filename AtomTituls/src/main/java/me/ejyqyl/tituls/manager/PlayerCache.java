package me.ejyqyl.tituls.manager;

import me.ejyqyl.tituls.AtomTitulsPlugin;
import me.ejyqyl.tituls.model.PlayerData;
import me.ejyqyl.tituls.model.Titul;
import me.ejyqyl.tituls.storage.TitleStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerCache {
    private final AtomTitulsPlugin plugin;
    private final TitleStorage storage;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Set<UUID> pendingActions = ConcurrentHashMap.newKeySet();

    public PlayerCache(@NotNull AtomTitulsPlugin plugin, @NotNull TitleStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public boolean tryAcquireAction(@NotNull UUID uuid) {
        return pendingActions.add(uuid);
    }

    public void releaseAction(@NotNull UUID uuid) {
        pendingActions.remove(uuid);
    }

    public CompletableFuture<PlayerData> loadSnapshot(@NotNull UUID uuid) {
        CompletableFuture<List<Titul>> titlesFuture = storage.loadPlayerTituls(uuid, plugin.getTitulManager());
        CompletableFuture<Titul> activeFuture = storage.loadActiveTitul(uuid, plugin.getTitulManager());
        CompletableFuture<String> sortFuture = storage.loadSortMode(uuid);

        return CompletableFuture.allOf(titlesFuture, activeFuture, sortFuture)
                .thenApply(v -> new PlayerData(uuid, titlesFuture.join(), activeFuture.join(), sortFuture.join()));
    }

    public CompletableFuture<Void> load(@NotNull UUID uuid) {
        return loadSnapshot(uuid).thenAccept(data -> cache.put(uuid, data))
                .exceptionally(ex -> {
                    plugin.getLogger().log(Level.WARNING, "Failed to load titles for " + uuid, ex);
                    return null;
                });
    }

    public void unload(@NotNull UUID uuid) {
        cache.remove(uuid);
        pendingActions.remove(uuid);
    }

    @Nullable
    public PlayerData getPlayerData(@NotNull UUID uuid) {
        return cache.get(uuid);
    }

    @NotNull
    public List<Titul> getUnlockedTituls(@NotNull UUID uuid) {
        PlayerData data = cache.get(uuid);
        return data != null ? data.getUnlockedTituls() : Collections.emptyList();
    }

    @Nullable
    public Titul getActiveTitul(@NotNull UUID uuid) {
        PlayerData data = cache.get(uuid);
        return data != null ? data.getActiveTitul() : null;
    }

    @Nullable
    public String getSortMode(@NotNull UUID uuid) {
        PlayerData data = cache.get(uuid);
        return data != null ? data.getSortMode() : null;
    }

    public boolean hasTitul(@NotNull UUID uuid, @NotNull String titleId) {
        PlayerData data = cache.get(uuid);
        return data != null && data.hasTitul(titleId);
    }

    public CompletableFuture<Void> setSortMode(@NotNull UUID uuid, @NotNull String sortMode) {
        return storage.setSortMode(uuid, sortMode).thenRun(() -> {
            PlayerData data = cache.get(uuid);
            if (data != null) {
                data.setSortMode(sortMode);
            }
        });
    }

    public CompletableFuture<Void> unlockTitul(@NotNull UUID uuid, @NotNull Titul titul, @Nullable Long obtainedAt) {
        long finalObtainedAt = obtainedAt != null ? obtainedAt : System.currentTimeMillis();
        Titul copy = new Titul(titul.getId(), titul.getName(), titul.getType(), titul.getRarity(), finalObtainedAt);

        return storage.addTitle(uuid, copy.getId(), copy.getType(), finalObtainedAt).thenRun(() -> {
            PlayerData data = cache.get(uuid);
            if (data != null) {
                data.addTitul(copy);
            }
        });
    }

    public CompletableFuture<Void> revokeTitul(@NotNull UUID uuid, @NotNull String titleId) {
        return storage.removeTitle(uuid, titleId)
                .thenCompose(v -> storage.removeActiveTitleIfMatches(uuid, titleId))
                .thenRun(() -> {
                    PlayerData data = cache.get(uuid);
                    if (data != null) {
                        data.removeTitul(titleId);
                    }
                });
    }

    public CompletableFuture<Void> activateTitul(@NotNull UUID uuid, @NotNull Titul titul) {
        Long obtainedAt = titul.getObtainedAt();
        if (obtainedAt == null) {
            long now = System.currentTimeMillis();
            return storage.updateUnlockedAt(uuid, titul.getId(), now)
                    .thenCompose(v -> storage.setActiveTitle(uuid, titul.getId(), titul.getType()))
                    .thenRun(() -> {
                        titul.setObtainedAt(now);
                        PlayerData data = cache.get(uuid);
                        if (data != null) {
                            data.setActiveTitul(titul);
                        }
                    });
        }

        return storage.setActiveTitle(uuid, titul.getId(), titul.getType()).thenRun(() -> {
            PlayerData data = cache.get(uuid);
            if (data != null) {
                data.setActiveTitul(titul);
            }
        });
    }

    public CompletableFuture<Void> clearActiveTitul(@NotNull UUID uuid) {
        return storage.removeActiveTitle(uuid).thenRun(() -> {
            PlayerData data = cache.get(uuid);
            if (data != null) {
                data.setActiveTitul(null);
            }
        });
    }
}
