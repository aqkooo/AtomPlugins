package com.ejyqyl.atomduels.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages arena storage, selection, and serialization.
 * Authored by ejyqyl.
 */
public class ArenaManager {

    private final Plugin plugin;
    private final File arenasFile;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public ArenaManager(Plugin plugin) {
        this.plugin = plugin;
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public void loadArenas() {
        arenas.clear();
        if (!arenasFile.exists()) {
            if (plugin.getResource("arenas.yml") != null) {
                try {
                    plugin.saveResource("arenas.yml", false);
                } catch (Exception ignored) {}
            }
            if (!arenasFile.exists()) {
                try {
                    arenasFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not create arenas.yml", e);
                    return;
                }
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(arenasFile);
        ConfigurationSection section = config.getConfigurationSection("arenas");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection aSec = section.getConfigurationSection(key);
            if (aSec == null) continue;

            Arena arena = new Arena(key);
            arena.setDisplayName(aSec.getString("display-name", key));
            arena.setEnabled(aSec.getBoolean("enabled", true));
            arena.setSpawn1(deserializeLocation(aSec.getConfigurationSection("spawn1")));
            arena.setSpawn2(deserializeLocation(aSec.getConfigurationSection("spawn2")));
            arena.setSpectatorSpawn(deserializeLocation(aSec.getConfigurationSection("spectator-spawn")));
            arena.setPos1(deserializeLocation(aSec.getConfigurationSection("pos1")));
            arena.setPos2(deserializeLocation(aSec.getConfigurationSection("pos2")));

            arenas.put(key.toLowerCase(), arena);
        }
        plugin.getLogger().info("Loaded " + arenas.size() + " arena(s).");
    }

    public void saveArenas() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection section = config.createSection("arenas");

        for (Arena arena : arenas.values()) {
            ConfigurationSection aSec = section.createSection(arena.getName());
            aSec.set("display-name", arena.getDisplayName());
            aSec.set("enabled", arena.isEnabled());
            if (arena.getSpawn1() != null) serializeLocation(aSec.createSection("spawn1"), arena.getSpawn1());
            if (arena.getSpawn2() != null) serializeLocation(aSec.createSection("spawn2"), arena.getSpawn2());
            if (arena.getSpectatorSpawn() != null) serializeLocation(aSec.createSection("spectator-spawn"), arena.getSpectatorSpawn());
            if (arena.getPos1() != null) serializeLocation(aSec.createSection("pos1"), arena.getPos1());
            if (arena.getPos2() != null) serializeLocation(aSec.createSection("pos2"), arena.getPos2());
        }

        try {
            config.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save arenas.yml", e);
        }
    }

    public Arena createArena(String name) {
        String key = name.toLowerCase();
        if (arenas.containsKey(key)) {
            return arenas.get(key);
        }
        Arena arena = new Arena(name);
        arenas.put(key, arena);
        saveArenas();
        return arena;
    }

    public boolean deleteArena(String name) {
        String key = name.toLowerCase();
        if (arenas.remove(key) != null) {
            saveArenas();
            return true;
        }
        return false;
    }

    public Arena getArena(String name) {
        if (name == null) return null;
        return arenas.get(name.toLowerCase());
    }

    public Collection<Arena> getArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public List<Arena> getAvailableArenas() {
        List<Arena> available = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.isEnabled() && !arena.isInUse() && arena.isConfigured()) {
                available.add(arena);
            }
        }
        return available;
    }

    public Arena getRandomAvailableArena() {
        List<Arena> available = getAvailableArenas();
        if (available.isEmpty()) {
            return null;
        }
        return available.get(random.nextInt(available.size()));
    }

    public Arena getArenaAt(Location loc) {
        if (loc == null) return null;
        for (Arena arena : arenas.values()) {
            if (arena.isInside(loc)) {
                return arena;
            }
        }
        return null;
    }

    public Arena getArenaByDuelId(UUID duelId) {
        if (duelId == null) return null;
        for (Arena arena : arenas.values()) {
            if (duelId.equals(arena.getCurrentDuelId())) {
                return arena;
            }
        }
        return null;
    }

    private void serializeLocation(ConfigurationSection sec, Location loc) {
        sec.set("world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        sec.set("x", loc.getX());
        sec.set("y", loc.getY());
        sec.set("z", loc.getZ());
        sec.set("yaw", loc.getYaw());
        sec.set("pitch", loc.getPitch());
    }

    private Location deserializeLocation(ConfigurationSection sec) {
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
}
