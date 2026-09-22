package ru.atomicsqd.atomregen.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import ru.atomicsqd.atomregen.AtomRegen;
import ru.atomicsqd.atomregen.model.CuboidRegion;
import ru.atomicsqd.atomregen.service.RegenEngine;
import ru.atomicsqd.atomregen.service.RegionManager;
import ru.atomicsqd.atomregen.util.ColorUtil;

/**
 * Listens for block placement, breaking, and liquid interactions inside auto-regenerating regions.
 */
public class BlockListener implements Listener {

    private final AtomRegen plugin;
    private final RegionManager regionManager;
    private final RegenEngine regenEngine;

    public BlockListener(AtomRegen plugin, RegionManager regionManager, RegenEngine regenEngine) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.regenEngine = regenEngine;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        CuboidRegion region = regionManager.getRegionAt(block.getLocation());
        if (region == null) return;

        Player player = event.getPlayer();
        if (!region.isAllowBlockPlace() && !player.hasPermission("atomregen.admin")) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.color(plugin.getConfig().getString("prefix", "") + "&cСтроительство в этом регионе запрещено!"));
            return;
        }

        // Queue block to vanish/restore back to snapshot
        regenEngine.queueBlock(block.getLocation(), true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        CuboidRegion region = regionManager.getRegionAt(block.getLocation());
        if (region == null) return;

        Player player = event.getPlayer();
        if (!region.isAllowBlockBreak() && !player.hasPermission("atomregen.admin")) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.color(plugin.getConfig().getString("prefix", "") + "&cРазрушение блоков в этом регионе запрещено!"));
            return;
        }

        // Suppress item and experience drops if configured
        if (!region.isDropBlocksOnBreak()) {
            event.setDropItems(false);
            event.setExpToDrop(0);
        }

        // Queue block to be restored back to snapshot
        regenEngine.queueBlock(block.getLocation(), false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block clicked = event.getBlockClicked();
        Block liquidBlock = clicked.getRelative(event.getBlockFace());
        CuboidRegion region = regionManager.getRegionAt(liquidBlock.getLocation());
        if (region == null) return;

        Player player = event.getPlayer();
        if (!region.isAllowBlockPlace() && !player.hasPermission("atomregen.admin")) {
            event.setCancelled(true);
            return;
        }

        // Queue the placed liquid block to vanish
        regenEngine.queueBlock(liquidBlock.getLocation(), true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Block clicked = event.getBlockClicked();
        CuboidRegion region = regionManager.getRegionAt(clicked.getLocation());
        if (region == null) return;

        Player player = event.getPlayer();
        if (!region.isAllowBlockBreak() && !player.hasPermission("atomregen.admin")) {
            event.setCancelled(true);
            return;
        }

        // Queue the removed liquid source block to restore
        regenEngine.queueBlock(clicked.getLocation(), false);
    }
}
