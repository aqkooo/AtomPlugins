package com.ejyqyl.glowcmd.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable model representing a spawn location.
 *
 * @author ejyqyl
 */
public final class SpawnPoint {

    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public SpawnPoint(@NotNull String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = Objects.requireNonNull(worldName, "worldName cannot be null");
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /**
     * Creates a SpawnPoint instance from a Bukkit Location.
     *
     * @param location Non-null Bukkit location
     * @return SpawnPoint instance
     */
    @NotNull
    public static SpawnPoint fromLocation(@NotNull Location location) {
        World world = Objects.requireNonNull(location.getWorld(), "Location world cannot be null");
        return new SpawnPoint(
                world.getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    /**
     * Resolves the Bukkit Location for this SpawnPoint.
     * Returns null if the target world is not loaded on the server.
     *
     * @return Bukkit Location or null if world not found
     */
    @Nullable
    public Location toLocation() {
        org.bukkit.Server server = Bukkit.getServer();
        if (server == null) {
            return null;
        }
        World world = server.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    @NotNull
    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpawnPoint that)) return false;
        return Double.compare(that.x, x) == 0 &&
                Double.compare(that.y, y) == 0 &&
                Double.compare(that.z, z) == 0 &&
                Float.compare(that.yaw, yaw) == 0 &&
                Float.compare(that.pitch, pitch) == 0 &&
                worldName.equals(that.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, x, y, z, yaw, pitch);
    }

    @Override
    public String toString() {
        return "SpawnPoint{" +
                "world='" + worldName + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                '}';
    }
}
