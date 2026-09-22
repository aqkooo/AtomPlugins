package com.ejyqyl.atomduels.arena;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;

/**
 * High-speed in-memory tracker of block modifications during a duel.
 * Allows 0.001s rollback without WorldEdit or heavy file I/O.
 * Authored by ejyqyl.
 */
public class BlockTracker {

    private final Map<Location, BlockData> originalBlocks = new HashMap<>();

    /**
     * Records the original state of a block before it gets altered, placed, or broken.
     * Only stores the very first state if called multiple times on the same location.
     */
    public synchronized void recordChange(Block block) {
        Location loc = block.getLocation();
        if (!originalBlocks.containsKey(loc)) {
            originalBlocks.put(loc.clone(), block.getBlockData().clone());
        }
    }

    /**
     * Rolls back all tracked blocks to their original state and clears the tracker.
     *
     * @return count of restored blocks
     */
    public synchronized int rollback() {
        int count = 0;
        for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet()) {
            Location loc = entry.getKey();
            BlockData data = entry.getValue();
            if (loc.getWorld() != null) {
                loc.getBlock().setBlockData(data, false);
                count++;
            }
        }
        originalBlocks.clear();
        return count;
    }

    public synchronized int getTrackedCount() {
        return originalBlocks.size();
    }

    public synchronized void clear() {
        originalBlocks.clear();
    }
}
