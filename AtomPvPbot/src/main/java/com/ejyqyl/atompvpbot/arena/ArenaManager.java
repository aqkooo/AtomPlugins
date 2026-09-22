package com.ejyqyl.atompvpbot.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages arena storage, loading, and runtime availability.
 *
 * @author ejyqyl
 */
public class ArenaManager {

    private final Plugin plugin;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private final File arenasFile;
    private FileConfiguration arenasConfig;

    public ArenaManager(Plugin plugin) {
        this.plugin = plugin;
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public void loadArenas() {
        arenas.clear();
        if (!arenasFile.exists()) {
            if (plugin.getResource("arenas.yml") != null) {
                plugin.saveResource("arenas.yml", false);
            }
        }

        this.arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
        ConfigurationSection sec = arenasConfig.getConfigurationSection("arenas");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            ConfigurationSection aSec = sec.getConfigurationSection(key);
            if (aSec == null) continue;

            Arena arena = new Arena(key);
            arena.setDisplayName(aSec.getString("display-name", key));
            arena.setEnabled(aSec.getBoolean("enabled", true));
            arena.setSpawn1(loadLocation(aSec.getConfigurationSection("spawn1")));
            arena.setSpawn2(loadLocation(aSec.getConfigurationSection("spawn2")));
            arena.setSpectatorSpawn(loadLocation(aSec.getConfigurationSection("spectator-spawn")));
            arena.setPos1(loadLocation(aSec.getConfigurationSection("pos1")));
            arena.setPos2(loadLocation(aSec.getConfigurationSection("pos2")));

            if (aSec.contains("allowed-kits")) {
                arena.getAllowedKits().addAll(aSec.getStringList("allowed-kits"));
            }

            arenas.put(key.toLowerCase(), arena);
        }
        plugin.getLogger().info("Loaded " + arenas.size() + " arenas for AtomPvPbot.");
    }

    public void saveArenas() {
        if (arenasConfig == null) arenasConfig = new YamlConfiguration();
        arenasConfig.set("arenas", null);

        for (Arena arena : arenas.values()) {
            String path = "arenas." + arena.getName();
            arenasConfig.set(path + ".display-name", arena.getDisplayName());
            arenasConfig.set(path + ".enabled", arena.isEnabled());
            saveLocation(arenasConfig, path + ".spawn1", arena.getSpawn1());
            saveLocation(arenasConfig, path + ".spawn2", arena.getSpawn2());
            saveLocation(arenasConfig, path + ".spectator-spawn", arena.getSpectatorSpawn());
            saveLocation(arenasConfig, path + ".pos1", arena.getPos1());
            saveLocation(arenasConfig, path + ".pos2", arena.getPos2());
            arenasConfig.set(path + ".allowed-kits", arena.getAllowedKits());
        }

        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save arenas.yml", e);
        }
    }

    public Arena getArena(String name) {
        if (name == null) return null;
        return arenas.get(name.toLowerCase());
    }

    public Arena createArena(String name) {
        Arena arena = new Arena(name);
        arenas.put(name.toLowerCase(), arena);
        saveArenas();
        return arena;
    }

    public boolean deleteArena(String name) {
        Arena removed = arenas.remove(name.toLowerCase());
        if (removed != null) {
            saveArenas();
            return true;
        }
        return false;
    }

    public Collection<Arena> getArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public Arena getAvailableArena() {
        List<Arena> pool = new ArrayList<>();
        for (Arena a : arenas.values()) {
            if (a.isEnabled() && !a.isInUse() && a.isConfigured()) {
                pool.add(a);
            }
        }
        if (pool.isEmpty()) return null;
        Collections.shuffle(pool);
        return pool.get(0);
    }

    public Arena getArenaAt(Location loc) {
        if (loc == null) return null;
        for (Arena arena : arenas.values()) {
            if (arena.contains(loc)) {
                return arena;
            }
        }
        return null;
    }

    private Location loadLocation(ConfigurationSection sec) {
        if (sec == null) return null;
        String worldName = sec.getString("world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = sec.getDouble("x");
        double y = sec.getDouble("y");
        double z = sec.getDouble("z");
        float yaw = (float) sec.getDouble("yaw", 0.0);
        float pitch = (float) sec.getDouble("pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void saveLocation(FileConfiguration conf, String path, Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        conf.set(path + ".world", loc.getWorld().getName());
        conf.set(path + ".x", loc.getX());
        conf.set(path + ".y", loc.getY());
        conf.set(path + ".z", loc.getZ());
        conf.set(path + ".yaw", loc.getYaw());
        conf.set(path + ".pitch", loc.getPitch());
    }
}
