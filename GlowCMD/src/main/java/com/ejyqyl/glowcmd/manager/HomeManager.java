package com.ejyqyl.glowcmd.manager;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.model.SpawnPoint;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-performance Home manager supporting multiple homes per player,
 * permission-based home limits, in-memory caching, and async disk persistence.
 *
 * @author ejyqyl
 */
public final class HomeManager {

    private final GlowCMD plugin;
    private final File homesFile;
    private final Map<UUID, Map<String, SpawnPoint>> playerHomes = new ConcurrentHashMap<>();

    private final AtomicBoolean isSaving = new AtomicBoolean(false);
    private final AtomicBoolean hasPendingSave = new AtomicBoolean(false);

    public HomeManager(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.homesFile = new File(dataDir, "homes.yml");
        load();
    }

    public synchronized void load() {
        playerHomes.clear();
        if (!homesFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(homesFile);
        for (String uuidStr : config.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            ConfigurationSection userSec = config.getConfigurationSection(uuidStr);
            if (userSec == null) continue;

            Map<String, SpawnPoint> homes = new ConcurrentHashMap<>();
            for (String homeName : userSec.getKeys(false)) {
                ConfigurationSection hSec = userSec.getConfigurationSection(homeName);
                if (hSec == null) continue;

                String world = hSec.getString("world");
                if (world == null) continue;

                double x = hSec.getDouble("x");
                double y = hSec.getDouble("y");
                double z = hSec.getDouble("z");
                float yaw = (float) hSec.getDouble("yaw", 0.0);
                float pitch = (float) hSec.getDouble("pitch", 0.0);

                homes.put(homeName.toLowerCase(), new SpawnPoint(world, x, y, z, yaw, pitch));
            }
            playerHomes.put(uuid, homes);
        }
    }

    public void setHome(@NotNull UUID playerId, @NotNull String name, @NotNull SpawnPoint point) {
        playerHomes.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(name.toLowerCase(), point);
        saveAsync();
    }

    @Nullable
    public SpawnPoint getHome(@NotNull UUID playerId, @NotNull String name) {
        Map<String, SpawnPoint> map = playerHomes.get(playerId);
        return map != null ? map.get(name.toLowerCase()) : null;
    }

    public boolean deleteHome(@NotNull UUID playerId, @NotNull String name) {
        Map<String, SpawnPoint> map = playerHomes.get(playerId);
        if (map != null && map.remove(name.toLowerCase()) != null) {
            saveAsync();
            return true;
        }
        return false;
    }

    @NotNull
    public Map<String, SpawnPoint> getHomes(@NotNull UUID playerId) {
        Map<String, SpawnPoint> map = playerHomes.get(playerId);
        return map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap();
    }

    public int getMaxHomes(@NotNull Player player) {
        if (player.hasPermission("glowcmd.homes.unlimited") || player.isOp()) {
            return 1000;
        }

        int max = 3; // default
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            String perm = pai.getPermission().toLowerCase();
            if (perm.startsWith("glowcmd.homes.") && pai.getValue()) {
                String val = perm.substring("glowcmd.homes.".length());
                try {
                    int count = Integer.parseInt(val);
                    if (count > max) {
                        max = count;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return max;
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
        for (Map.Entry<UUID, Map<String, SpawnPoint>> userEntry : playerHomes.entrySet()) {
            String uuidStr = userEntry.getKey().toString();
            ConfigurationSection userSec = config.createSection(uuidStr);
            for (Map.Entry<String, SpawnPoint> homeEntry : userEntry.getValue().entrySet()) {
                ConfigurationSection hSec = userSec.createSection(homeEntry.getKey());
                SpawnPoint sp = homeEntry.getValue();
                hSec.set("world", sp.getWorldName());
                hSec.set("x", sp.getX());
                hSec.set("y", sp.getY());
                hSec.set("z", sp.getZ());
                hSec.set("yaw", sp.getYaw());
                hSec.set("pitch", sp.getPitch());
            }
        }

        try {
            homesFile.getParentFile().mkdirs();
            config.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save homes data: " + e.getMessage());
        }
    }
}
