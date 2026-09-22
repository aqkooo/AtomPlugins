package ru.atomicsqd.atomregen.model;

import java.util.Objects;

/**
 * Represents a block scheduled for regeneration.
 */
public class PendingRegen implements Comparable<PendingRegen> {

    private final String worldName;
    private final BlockVector vector;
    private long scheduledTimeMillis;
    private final String targetBlockDataString;
    private final boolean temporaryPlaced;

    public PendingRegen(String worldName, BlockVector vector, long scheduledTimeMillis,
                        String targetBlockDataString, boolean temporaryPlaced) {
        this.worldName = worldName;
        this.vector = vector;
        this.scheduledTimeMillis = scheduledTimeMillis;
        this.targetBlockDataString = targetBlockDataString;
        this.temporaryPlaced = temporaryPlaced;
    }

    public String getWorldName() {
        return worldName;
    }

    public BlockVector getVector() {
        return vector;
    }

    public long getScheduledTimeMillis() {
        return scheduledTimeMillis;
    }

    public void setScheduledTimeMillis(long scheduledTimeMillis) {
        this.scheduledTimeMillis = scheduledTimeMillis;
    }

    public String getTargetBlockDataString() {
        return targetBlockDataString;
    }

    public boolean isTemporaryPlaced() {
        return temporaryPlaced;
    }

    @Override
    public int compareTo(PendingRegen other) {
        return Long.compare(this.scheduledTimeMillis, other.scheduledTimeMillis);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PendingRegen that = (PendingRegen) o;
        return Objects.equals(worldName, that.worldName) && Objects.equals(vector, that.vector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, vector);
    }
}
