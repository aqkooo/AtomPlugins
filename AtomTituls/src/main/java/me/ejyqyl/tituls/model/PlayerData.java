package me.ejyqyl.tituls.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerData {
    private final UUID uuid;
    private final List<Titul> unlockedTituls = new CopyOnWriteArrayList<>();
    private volatile Titul activeTitul;
    private volatile String sortMode;

    public PlayerData(@NotNull UUID uuid, @Nullable Collection<Titul> tituls, @Nullable Titul activeTitul, @Nullable String sortMode) {
        this.uuid = uuid;
        if (tituls != null) {
            this.unlockedTituls.addAll(tituls);
        }
        this.activeTitul = activeTitul;
        this.sortMode = sortMode;
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    @NotNull
    public List<Titul> getUnlockedTituls() {
        return new ArrayList<>(unlockedTituls);
    }

    public boolean hasTitul(@NotNull String titleId) {
        for (Titul titul : unlockedTituls) {
            if (titul.getId().equalsIgnoreCase(titleId)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public Titul getTitul(@NotNull String titleId) {
        for (Titul titul : unlockedTituls) {
            if (titul.getId().equalsIgnoreCase(titleId)) {
                return titul;
            }
        }
        return null;
    }

    public void addTitul(@NotNull Titul titul) {
        if (!hasTitul(titul.getId())) {
            unlockedTituls.add(titul);
        }
    }

    public void removeTitul(@NotNull String titleId) {
        unlockedTituls.removeIf(t -> t.getId().equalsIgnoreCase(titleId));
        if (activeTitul != null && activeTitul.getId().equalsIgnoreCase(titleId)) {
            activeTitul = null;
        }
    }

    @Nullable
    public Titul getActiveTitul() {
        return activeTitul;
    }

    public void setActiveTitul(@Nullable Titul activeTitul) {
        this.activeTitul = activeTitul;
    }

    @Nullable
    public String getSortMode() {
        return sortMode;
    }

    public void setSortMode(@Nullable String sortMode) {
        this.sortMode = sortMode;
    }
}
