package com.ejyqyl.glowcmd.config;

import com.ejyqyl.glowcmd.GlowCMD;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Manages plugin configuration loading and in-memory cached properties.
 *
 * @author ejyqyl
 */
public final class ConfigManager {

    private final GlowCMD plugin;

    private boolean spawnEnabled = true;
    private boolean firstSpawnEnabled = true;
    private boolean teleportOnFirstJoin = true;
    private boolean teleportOnDeath = false;
    private boolean respectBedSpawn = false;

    public ConfigManager(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Reloads config.yml and updates in-memory cached variables.
     */
    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.spawnEnabled = config.getBoolean("spawn.enabled", true);
        this.firstSpawnEnabled = config.getBoolean("spawn.first-spawn.enabled", true);
        this.teleportOnFirstJoin = config.getBoolean("spawn.teleport-on-first-join", true);
        this.teleportOnDeath = config.getBoolean("spawn.teleport-on-death", false);
        this.respectBedSpawn = config.getBoolean("spawn.respect-bed-spawn", false);
    }

    public boolean isSpawnEnabled() {
        return spawnEnabled;
    }

    public boolean isFirstSpawnEnabled() {
        return firstSpawnEnabled;
    }

    public boolean isTeleportOnFirstJoin() {
        return teleportOnFirstJoin;
    }

    public boolean isTeleportOnDeath() {
        return teleportOnDeath;
    }

    public boolean isRespectBedSpawn() {
        return respectBedSpawn;
    }
}
