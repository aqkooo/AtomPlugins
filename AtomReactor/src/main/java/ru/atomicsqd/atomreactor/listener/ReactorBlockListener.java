package ru.atomicsqd.atomreactor.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ru.atomicsqd.atomreactor.AtomReactor;
import ru.atomicsqd.atomreactor.config.ReactorLevel;
import ru.atomicsqd.atomreactor.gui.ReactorGUI;
import ru.atomicsqd.atomreactor.model.Reactor;
import ru.atomicsqd.atomreactor.service.ReactorManager;
import ru.atomicsqd.atomreactor.util.ColorUtil;

/**
 * Handles block placement, destruction, and interactions with Reactor blocks.
 */
public class ReactorBlockListener implements Listener {

    private final AtomReactor plugin;
    private final ReactorManager reactorManager;

    public ReactorBlockListener(AtomReactor plugin, ReactorManager reactorManager) {
        this.plugin = plugin;
        this.reactorManager = reactorManager;
    }

    private String getPrefix() {
        return plugin.getConfigManager().getPrefix();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!reactorManager.isReactorItem(item)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("atomreactor.use")) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.component(getPrefix() + plugin.getConfigManager().getMessage("no-permission")));
            return;
        }

        int level = reactorManager.getReactorItemLevel(item);
        Block block = event.getBlockPlaced();

        Reactor reactor = reactorManager.createReactor(player, block.getLocation(), level);
        ReactorLevel rLevel = plugin.getConfigManager().getLevel(level);

        player.playSound(block.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
        String msg = plugin.getConfigManager().getMessage("reactor-placed")
                .replace("%level_name%", rLevel != null ? rLevel.getName() : "Ур. " + level)
                .replace("%level%", String.valueOf(level));
        player.sendMessage(ColorUtil.component(getPrefix() + msg));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Reactor reactor = reactorManager.getReactorAt(block.getLocation());
        if (reactor == null) return;

        Player player = event.getPlayer();
        boolean isOwner = reactor.getOwnerUuid().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("atomreactor.admin");

        if (!isOwner && !isAdmin) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.component(getPrefix() + plugin.getConfigManager().getMessage("not-owner")));
            return;
        }

        // Drop reactor item with stored level
        event.setDropItems(false);
        event.setExpToDrop(0);

        ItemStack reactorItem = reactorManager.createReactorItem(reactor.getLevel());
        block.getWorld().dropItemNaturally(block.getLocation(), reactorItem);

        reactorManager.removeReactor(reactor);

        player.playSound(block.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
        player.sendMessage(ColorUtil.component(getPrefix() + plugin.getConfigManager().getMessage("reactor-broken")));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Reactor reactor = reactorManager.getReactorAt(clicked.getLocation());
        if (reactor == null) return;

        Player player = event.getPlayer();
        boolean isOwner = reactor.getOwnerUuid().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("atomreactor.admin");

        if (!isOwner && !isAdmin) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.component(getPrefix() + plugin.getConfigManager().getMessage("not-owner")));
            return;
        }

        event.setCancelled(true);
        player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1.0f, 1.2f);
        ReactorGUI.open(player, reactor, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Protect reactors from explosion destruction
        event.blockList().removeIf(b -> reactorManager.getReactorAt(b.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> reactorManager.getReactorAt(b.getLocation()) != null);
    }
}
