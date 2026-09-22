package ru.atomicsqd.atomreactor.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * Data model for an active placed Reactor.
 */
public class Reactor {

    private final UUID id;
    private final UUID ownerUuid;
    private String ownerName;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;

    private int level;
    private boolean active;
    private double storedBalance;
    private long lastGenerationTime;

    private transient UUID hologramEntityUuid;

    public Reactor(UUID id, UUID ownerUuid, String ownerName,
                   String worldName, int x, int y, int z,
                   int level) {
        this.id = id != null ? id : UUID.randomUUID();
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.level = Math.max(1, level);
        this.active = true;
        this.storedBalance = 0.0;
        this.lastGenerationTime = System.currentTimeMillis();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getWorldName() {
        return worldName;
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

    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getStoredBalance() {
        return storedBalance;
    }

    public void setStoredBalance(double storedBalance) {
        this.storedBalance = Math.max(0.0, storedBalance);
    }

    public void depositStored(double amount, double maxStorage) {
        this.storedBalance = Math.min(maxStorage, this.storedBalance + amount);
    }

    public double withdrawAllStored() {
        double amount = this.storedBalance;
        this.storedBalance = 0.0;
        return amount;
    }

    public long getLastGenerationTime() {
        return lastGenerationTime;
    }

    public void setLastGenerationTime(long lastGenerationTime) {
        this.lastGenerationTime = lastGenerationTime;
    }

    public UUID getHologramEntityUuid() {
        return hologramEntityUuid;
    }

    public void setHologramEntityUuid(UUID hologramEntityUuid) {
        this.hologramEntityUuid = hologramEntityUuid;
    }
}
