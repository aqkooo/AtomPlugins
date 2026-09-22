package ru.atomicsqd.atomregen.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import ru.atomicsqd.atomregen.AtomRegen;
import ru.atomicsqd.atomregen.model.BlockVector;
import ru.atomicsqd.atomregen.model.CuboidRegion;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all defined regions, their JSON persistence, and snapshot restoration.
 */
public class RegionManager {

    private final AtomRegen plugin;
    private final File regionsFolder;
    private final Gson gson;
    private final Map<String, CuboidRegion> regions = new ConcurrentHashMap<>();

    public RegionManager(AtomRegen plugin) {
        this.plugin = plugin;
        this.regionsFolder = new File(plugin.getDataFolder(), "regions");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        if (!regionsFolder.exists()) {
            regionsFolder.mkdirs();
        }
    }

    public void loadRegions() {
        regions.clear();
        File[] files = regionsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                CuboidRegion region = gson.fromJson(reader, CuboidRegion.class);
                if (region != null && region.getName() != null) {
                    regions.put(region.getName().toLowerCase(), region);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load region file: " + file.getName(), e);
            }
        }
        plugin.getLogger().info("Loaded " + regions.size() + " auto-regenerating region(s).");
    }

    public void saveRegion(CuboidRegion region) {
        File file = new File(regionsFolder, region.getName().toLowerCase() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(region, writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save region: " + region.getName(), e);
        }
    }

    public CuboidRegion createRegion(String name, World world, BlockVector p1, BlockVector p2, long delaySeconds) {
        int minX = Math.min(p1.getX(), p2.getX());
        int maxX = Math.max(p1.getX(), p2.getX());
        int minY = Math.min(p1.getY(), p2.getY());
        int maxY = Math.max(p1.getY(), p2.getY());
        int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxZ = Math.max(p1.getZ(), p2.getZ());

        CuboidRegion region = new CuboidRegion(name, world.getName(), minX, minY, minZ, maxX, maxY, maxZ, delaySeconds);

        // Capture snapshot of current blocks
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    region.setSnapshotData(x, y, z, block.getBlockData().getAsString());
                }
            }
        }

        regions.put(name.toLowerCase(), region);
        saveRegion(region);
        return region;
    }

    public boolean deleteRegion(String name) {
        CuboidRegion removed = regions.remove(name.toLowerCase());
        if (removed != null) {
            File file = new File(regionsFolder, name.toLowerCase() + ".json");
            if (file.exists()) {
                file.delete();
            }
            return true;
        }
        return false;
    }

    public CuboidRegion getRegion(String name) {
        if (name == null) return null;
        return regions.get(name.toLowerCase());
    }

    public CuboidRegion getRegionAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        for (CuboidRegion region : regions.values()) {
            if (region.contains(loc)) {
                return region;
            }
        }
        return null;
    }

    public Collection<CuboidRegion> getAllRegions() {
        return Collections.unmodifiableCollection(regions.values());
    }

    /**
     * Instantly forces all blocks in the region back to its original snapshot.
     */
    public int resetRegion(CuboidRegion region) {
        World world = Bukkit.getWorld(region.getWorldName());
        if (world == null) return 0;

        int restored = 0;
        for (Map.Entry<String, String> entry : region.getSnapshot().entrySet()) {
            String[] split = entry.getKey().split(",");
            int x = Integer.parseInt(split[0]);
            int y = Integer.parseInt(split[1]);
            int z = Integer.parseInt(split[2]);

            Block block = world.getBlockAt(x, y, z);
            try {
                BlockData targetData = Bukkit.createBlockData(entry.getValue());
                block.setBlockData(targetData, false);
                restored++;
            } catch (Exception ignored) {}
        }
        return restored;
    }
}
