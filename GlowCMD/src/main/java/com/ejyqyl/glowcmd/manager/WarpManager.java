package com.ejyqyl.glowcmd.manager;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.model.SpawnPoint;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-performance Warp manager supporting in-memory caching and async disk persistence.
 *
 * @author ejyqyl
 */
public final class WarpManager {

    private final GlowCMD plugin;
    private final File warpsFile;
    private final Map<String, SpawnPoint> warps = new ConcurrentHashMap<>();

    private final AtomicBoolean isSaving = new AtomicBoolean(false);
    private final AtomicBoolean hasPendingSave = new AtomicBoolean(false);

    public WarpManager(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.warpsFile = new File(dataDir, "warps.yml");
        load();
    }

    public synchronized void load() {
        warps.clear();
        if (!warpsFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
        for (String name : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(name);
            if (sec == null) continue;

            String world = sec.getString("world");
            if (world == null) continue;

            double x = sec.getDouble("x");
            double y = sec.getDouble("y");
            double z = sec.getDouble("z");
            float yaw = (float) sec.getDouble("yaw", 0.0);
            float pitch = (float) sec.getDouble("pitch", 0.0);

            warps.put(name.toLowerCase(), new SpawnPoint(world, x, y, z, yaw, pitch));
        }
    }

    public void setWarp(@NotNull String name, @NotNull SpawnPoint point) {
        warps.put(name.toLowerCase(), point);
        saveAsync();
    }

    @Nullable
    public SpawnPoint getWarp(@NotNull String name) {
        return warps.get(name.toLowerCase());
    }

    public boolean deleteWarp(@NotNull String name) {
        if (warps.remove(name.toLowerCase()) != null) {
            saveAsync();
            return true;
        }
        return false;
    }

    @NotNull
    public Map<String, SpawnPoint> getWarps() {
        return Collections.unmodifiableMap(warps);
    }

    public void saveAsync() {
        if (plugin.isEnabled()) {
            hasPendingSave.set(true);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, this::executeSave);
        } else {
            saveSync();
        }
    }

    private void executeSave() {
        if (!isSaving.compareAndSet(false, true)) {
            return;
        }

        try {
            do {
                hasPendingSave.set(false);
                saveSync();
            } while (hasPendingSave.get());
        } finally {
            isSaving.set(false);
        }
    }

    public synchronized void saveSync() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, SpawnPoint> entry : warps.entrySet()) {
            ConfigurationSection sec = config.createSection(entry.getKey());
            SpawnPoint sp = entry.getValue();
            sec.set("world", sp.getWorldName());
            sec.set("x", sp.getX());
            sec.set("y", sp.getY());
            sec.set("z", sp.getZ());
            sec.set("yaw", sp.getYaw());
            sec.set("pitch", sp.getPitch());
        }

        try {
            warpsFile.getParentFile().mkdirs();
            config.save(warpsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save warps data: " + e.getMessage());
        }
    }
}
