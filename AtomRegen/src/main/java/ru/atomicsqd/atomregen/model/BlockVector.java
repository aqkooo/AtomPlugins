package ru.atomicsqd.atomregen.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

/**
 * Immutable block coordinate vector.
 */
public final class BlockVector {

    private final int x;
    private final int y;
    private final int z;

    public BlockVector(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static BlockVector fromLocation(Location loc) {
        return new BlockVector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public static BlockVector fromBlock(Block block) {
        return new BlockVector(block.getX(), block.getY(), block.getZ());
    }

    public static BlockVector fromString(String str) {
        String[] split = str.split(",");
        if (split.length != 3) {
            throw new IllegalArgumentException("Invalid BlockVector string: " + str);
        }
        return new BlockVector(
                Integer.parseInt(split[0].trim()),
                Integer.parseInt(split[1].trim()),
                Integer.parseInt(split[2].trim())
        );
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

    public Block toBlock(World world) {
        return world.getBlockAt(x, y, z);
    }

    public String serialize() {
        return x + "," + y + "," + z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockVector that = (BlockVector) o;
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
