package ru.atomicsqd.atomregen.model;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a defined cuboid region with an immutable snapshot of original blocks.
 */
public class CuboidRegion {

    private final String name;
    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    private long regenDelaySeconds;
    private boolean dropBlocksOnBreak;
    private boolean dropBlocksOnExplode;
    private boolean allowBlockPlace;
    private boolean allowBlockBreak;

    /**
     * Map of "x,y,z" coordinates to serialized BlockData string (e.g. "minecraft:stone", "minecraft:air").
     */
    private final Map<String, String> snapshot;

    public CuboidRegion(String name, String worldName,
                        int minX, int minY, int minZ,
                        int maxX, int maxY, int maxZ,
                        long regenDelaySeconds) {
        this.name = name;
        this.worldName = worldName;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.regenDelaySeconds = Math.max(1, regenDelaySeconds);
        this.dropBlocksOnBreak = false;
        this.dropBlocksOnExplode = false;
        this.allowBlockPlace = true;
        this.allowBlockBreak = true;
        this.snapshot = new HashMap<>();
    }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
    }

    public boolean contains(int x, int y, int z, String world) {
        if (!this.worldName.equalsIgnoreCase(world)) return false;
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public int getVolume() {
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public String getSnapshotData(int x, int y, int z) {
        String key = x + "," + y + "," + z;
        return snapshot.get(key);
    }

    public void setSnapshotData(int x, int y, int z, String blockDataString) {
        snapshot.put(x + "," + y + "," + z, blockDataString);
    }

    public Map<String, String> getSnapshot() {
        return Collections.unmodifiableMap(snapshot);
    }

    public void putAllSnapshot(Map<String, String> data) {
        if (data != null) {
            snapshot.putAll(data);
        }
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public long getRegenDelaySeconds() {
        return regenDelaySeconds;
    }

    public void setRegenDelaySeconds(long regenDelaySeconds) {
        this.regenDelaySeconds = Math.max(1, regenDelaySeconds);
    }

    public boolean isDropBlocksOnBreak() {
        return dropBlocksOnBreak;
    }

    public void setDropBlocksOnBreak(boolean dropBlocksOnBreak) {
        this.dropBlocksOnBreak = dropBlocksOnBreak;
    }

    public boolean isDropBlocksOnExplode() {
        return dropBlocksOnExplode;
    }

    public void setDropBlocksOnExplode(boolean dropBlocksOnExplode) {
        this.dropBlocksOnExplode = dropBlocksOnExplode;
    }

    public boolean isAllowBlockPlace() {
        return allowBlockPlace;
    }

    public void setAllowBlockPlace(boolean allowBlockPlace) {
        this.allowBlockPlace = allowBlockPlace;
    }

    public boolean isAllowBlockBreak() {
        return allowBlockBreak;
    }

    public void setAllowBlockBreak(boolean allowBlockBreak) {
        this.allowBlockBreak = allowBlockBreak;
    }
}
