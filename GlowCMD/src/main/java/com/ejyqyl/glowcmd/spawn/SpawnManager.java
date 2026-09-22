package com.ejyqyl.glowcmd.spawn;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.model.SpawnPoint;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-performance Spawn & First-Spawn Manager.
 * Keeps spawn data in memory for O(1) query access, performs non-blocking asynchronous
 * persistence to plugins/GlowCMD/data/spawn.yml, and handles nonexistent worlds safely.
 *
 * @author ejyqyl
 */
public final class SpawnManager {

    private final GlowCMD plugin;
    private final File spawnFile;

    private volatile SpawnPoint spawnPoint;
    private volatile SpawnPoint firstSpawnPoint;

    private final AtomicBoolean isSaving = new AtomicBoolean(false);
    private final AtomicBoolean hasPendingSave = new AtomicBoolean(false);

    public SpawnManager(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.spawnFile = new File(dataDir, "spawn.yml");
        load();
    }

    /**
     * Loads spawn and first-spawn points from plugins/GlowCMD/data/spawn.yml into memory.
     * Safely logs a warning if a configured world is not found without crashing.
     */
    public synchronized void load() {
        if (!spawnFile.exists()) {
            this.spawnPoint = null;
            this.firstSpawnPoint = null;
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(spawnFile);

        this.spawnPoint = parseSpawnPoint(config.getConfigurationSection("spawn"), "spawn");
        this.firstSpawnPoint = parseSpawnPoint(config.getConfigurationSection("first-spawn"), "first-spawn");
    }

    @Nullable
    private SpawnPoint parseSpawnPoint(@Nullable ConfigurationSection section, @NotNull String sectionName) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world");
        if (worldName == null || worldName.trim().isEmpty()) {
            return null;
        }

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0.0);
        float pitch = (float) section.getDouble("pitch", 0.0);

        // Verify if world currently exists; if not, log warning but keep SpawnPoint in memory
        org.bukkit.Server server = Bukkit.getServer();
        if (server != null && server.getWorld(worldName) == null) {
            plugin.getLogger().warning("Failed to load " + sectionName + " world: " + worldName);
        }

        return new SpawnPoint(worldName, x, y, z, yaw, pitch);
    }

    @Nullable
    public SpawnPoint getSpawn() {
        return spawnPoint;
    }

    public void setSpawn(@NotNull SpawnPoint point) {
        this.spawnPoint = point;
        saveAsync();
    }

    @Nullable
    public SpawnPoint getFirstSpawn() {
        return firstSpawnPoint;
    }

    public void setFirstSpawn(@NotNull SpawnPoint point) {
        this.firstSpawnPoint = point;
        saveAsync();
    }

    /**
     * Schedules an asynchronous save to data/spawn.yml to avoid blocking the main server thread.
     */
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

    /**
     * Synchronously persists in-memory spawn points to plugins/GlowCMD/data/spawn.yml.
     */
    public synchronized void saveSync() {
        YamlConfiguration config = new YamlConfiguration();

        if (spawnPoint != null) {
            ConfigurationSection sec = config.createSection("spawn");
            sec.set("world", spawnPoint.getWorldName());
            sec.set("x", spawnPoint.getX());
            sec.set("y", spawnPoint.getY());
            sec.set("z", spawnPoint.getZ());
            sec.set("yaw", spawnPoint.getYaw());
            sec.set("pitch", spawnPoint.getPitch());
        }

        if (firstSpawnPoint != null) {
            ConfigurationSection sec = config.createSection("first-spawn");
            sec.set("world", firstSpawnPoint.getWorldName());
            sec.set("x", firstSpawnPoint.getX());
            sec.set("y", firstSpawnPoint.getY());
            sec.set("z", firstSpawnPoint.getZ());
            sec.set("yaw", firstSpawnPoint.getYaw());
            sec.set("pitch", firstSpawnPoint.getPitch());
        }

        try {
            File parent = spawnFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(spawnFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save spawn data to " + spawnFile.getPath() + ": " + e.getMessage());
        }
    }
}
