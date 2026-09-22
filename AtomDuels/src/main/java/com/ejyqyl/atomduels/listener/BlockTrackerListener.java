package com.ejyqyl.atomduels.listener;

import com.ejyqyl.atomduels.arena.Arena;
import com.ejyqyl.atomduels.arena.ArenaManager;
import com.ejyqyl.atomduels.duel.ActiveDuel;
import com.ejyqyl.atomduels.duel.DuelManager;
import com.ejyqyl.atomduels.duel.DuelState;
import com.ejyqyl.atomduels.rules.Rule;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Captures block modifications in real time for 0.001s rollback and enforces block rules.
 * Authored by ejyqyl.
 */
public class BlockTrackerListener implements Listener {

    private final DuelManager duelManager;
    private final ArenaManager arenaManager;

    public BlockTrackerListener(DuelManager duelManager, ArenaManager arenaManager) {
        this.duelManager = duelManager;
        this.arenaManager = arenaManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ActiveDuel duel = duelManager.getDuel(player.getUniqueId());

        if (duel != null) {
            if (duel.getState() != DuelState.IN_FIGHT || !duel.getRuleSet().isRuleEnabled(Rule.BLOCK_PLACE)) {
                event.setCancelled(true);
                return;
            }
            duel.getBlockTracker().recordChange(event.getBlock());
            return;
        }

        // Prevent non-duelists from placing blocks inside an arena
        Arena arena = arenaManager.getArenaAt(event.getBlock().getLocation());
        if (arena != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ActiveDuel duel = duelManager.getDuel(player.getUniqueId());

        if (duel != null) {
            if (duel.getState() != DuelState.IN_FIGHT || !duel.getRuleSet().isRuleEnabled(Rule.BLOCK_BREAK)) {
                event.setCancelled(true);
                return;
            }
            duel.getBlockTracker().recordChange(event.getBlock());
            return;
        }

        // Prevent non-duelists from breaking blocks inside an arena
        Arena arena = arenaManager.getArenaAt(event.getBlock().getLocation());
        if (arena != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Arena arena = arenaManager.getArenaAt(event.getLocation());
        if (arena != null) {
            ActiveDuel duel = arena.getCurrentDuelId() != null ? duelManager.getDuelById(arena.getCurrentDuelId()) : null;
            if (duel != null) {
                for (Block b : event.blockList()) {
                    duel.getBlockTracker().recordChange(b);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Arena arena = arenaManager.getArenaAt(event.getBlock().getLocation());
        if (arena != null) {
            ActiveDuel duel = arena.getCurrentDuelId() != null ? duelManager.getDuelById(arena.getCurrentDuelId()) : null;
            if (duel != null) {
                for (Block b : event.blockList()) {
                    duel.getBlockTracker().recordChange(b);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }
}
