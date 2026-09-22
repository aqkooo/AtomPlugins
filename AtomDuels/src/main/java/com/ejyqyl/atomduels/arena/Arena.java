package com.ejyqyl.atomduels.arena;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Model representing a duel arena.
 * Authored by ejyqyl.
 */
public class Arena {

    private final String name;
    private String displayName;
    private Location spawn1;
    private Location spawn2;
    private Location spectatorSpawn;
    private Location pos1;
    private Location pos2;
    private boolean enabled;
    private boolean inUse;
    private UUID currentDuelId;

    public Arena(String name) {
        this.name = name;
        this.displayName = name;
        this.enabled = true;
        this.inUse = false;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Location getSpawn1() {
        return spawn1;
    }

    public void setSpawn1(Location spawn1) {
        this.spawn1 = spawn1;
    }

    public Location getSpawn2() {
        return spawn2;
    }

    public void setSpawn2(Location spawn2) {
        this.spawn2 = spawn2;
    }

    public Location getSpectatorSpawn() {
        return spectatorSpawn != null ? spectatorSpawn : (spawn1 != null ? spawn1 : spawn2);
    }

    public void setSpectatorSpawn(Location spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
        if (!inUse) {
            this.currentDuelId = null;
        }
    }

    public UUID getCurrentDuelId() {
        return currentDuelId;
    }

    public void setCurrentDuelId(UUID currentDuelId) {
        this.currentDuelId = currentDuelId;
    }

    public boolean isConfigured() {
        return spawn1 != null && spawn2 != null && spawn1.getWorld() != null && spawn2.getWorld() != null;
    }

    public boolean isInside(Location loc) {
        if (loc == null || loc.getWorld() == null || pos1 == null || pos2 == null) {
            return false;
        }
        if (!loc.getWorld().equals(pos1.getWorld()) || !loc.getWorld().equals(pos2.getWorld())) {
            return false;
        }
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        return loc.getX() >= minX && loc.getX() <= maxX &&
               loc.getY() >= minY && loc.getY() <= maxY &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
}
