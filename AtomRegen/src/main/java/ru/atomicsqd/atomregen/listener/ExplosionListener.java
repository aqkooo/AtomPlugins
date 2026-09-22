package ru.atomicsqd.atomregen.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import ru.atomicsqd.atomregen.AtomRegen;
import ru.atomicsqd.atomregen.model.CuboidRegion;
import ru.atomicsqd.atomregen.service.RegenEngine;
import ru.atomicsqd.atomregen.service.RegionManager;

import java.util.Iterator;

/**
 * Listens for explosions (TNT, Crystals, Anchors, Wind Charges, Beds) and schedules affected blocks for regeneration.
 */
public class ExplosionListener implements Listener {

    private final AtomRegen plugin;
    private final RegionManager regionManager;
    private final RegenEngine regenEngine;

    public ExplosionListener(AtomRegen plugin, RegionManager regionManager, RegenEngine regenEngine) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.regenEngine = regenEngine;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosionBlocks(event.blockList().iterator());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosionBlocks(event.blockList().iterator());
    }

    private void handleExplosionBlocks(Iterator<Block> it) {
        while (it.hasNext()) {
            Block block = it.next();
            CuboidRegion region = regionManager.getRegionAt(block.getLocation());
            if (region != null) {
                // Queue block restoration
                regenEngine.queueBlock(block.getLocation(), false);

                // If drops disabled, remove from explosion list and break silently to prevent entity item lag
                if (!region.isDropBlocksOnExplode()) {
                    it.remove();
                    block.setType(Material.AIR, false);
                }
            }
        }
    }
}
