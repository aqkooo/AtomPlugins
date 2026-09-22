package ru.atomicsqd.atomreactor.listener;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import ru.atomicsqd.atomreactor.AtomReactor;
import ru.atomicsqd.atomreactor.config.ReactorLevel;
import ru.atomicsqd.atomreactor.economy.EconomyService;
import ru.atomicsqd.atomreactor.gui.ReactorGUI;
import ru.atomicsqd.atomreactor.model.Reactor;
import ru.atomicsqd.atomreactor.service.HologramService;
import ru.atomicsqd.atomreactor.service.ReactorManager;
import ru.atomicsqd.atomreactor.util.ColorUtil;

/**
 * Handles interactions inside ReactorGUI.
 */
public class GUIListener implements Listener {

    private final AtomReactor plugin;
    private final ReactorManager reactorManager;
    private final HologramService hologramService;
    private final EconomyService economyService;

    public GUIListener(AtomReactor plugin, ReactorManager reactorManager,
                       HologramService hologramService, EconomyService economyService) {
        this.plugin = plugin;
        this.reactorManager = reactorManager;
        this.hologramService = hologramService;
        this.economyService = economyService;
    }

    private String getPrefix() {
        return plugin.getConfigManager().getPrefix();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ReactorGUI gui)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Reactor reactor = gui.getReactor();
        int slot = event.getRawSlot();

        // 1. Upgrade Button (Slot 20)
        if (slot == 20) {
            handleUpgrade(player, reactor, gui);
        }
        // 2. Collect Bank Button (Slot 22)
        else if (slot == 22) {
            handleCollect(player, reactor, gui);
        }
        // 3. Toggle Active (Slot 24)
        else if (slot == 24) {
            handleToggle(player, reactor, gui);
        }
    }

    private void handleUpgrade(Player player, Reactor reactor, ReactorGUI gui) {
        ReactorLevel nextLevel = plugin.getConfigManager().getNextLevel(reactor.getLevel());
        if (nextLevel == null) {
            player.sendMessage(ColorUtil.component(getPrefix() + plugin.getConfigManager().getMessage("max-level-reached")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        double cost = nextLevel.getUpgradeCost();
        if (!economyService.has(player, cost)) {
            String msg = plugin.getConfigManager().getMessage("not-enough-money")
                    .replace("%cost%", String.format("%.1f", cost))
                    .replace("%currency%", plugin.getConfigManager().getCurrencySymbol());
            player.sendMessage(ColorUtil.component(getPrefix() + msg));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Process purchase
        economyService.withdraw(player, cost);
        reactor.setLevel(nextLevel.getLevel());
        reactorManager.saveReactors();
        hologramService.updateHologram(reactor);

        // Visual and auditory feedback
        Location rLoc = reactor.getLocation();
        if (rLoc != null && rLoc.getWorld() != null) {
            rLoc.getWorld().spawnParticle(Particle.FIREWORK, rLoc.clone().add(0.5, 1.2, 0.5), 25, 0.4, 0.4, 0.4, 0.05);
        }
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);

        String success = plugin.getConfigManager().getMessage("upgraded")
                .replace("%level_name%", nextLevel.getName())
                .replace("%level%", String.valueOf(nextLevel.getLevel()));
        player.sendMessage(ColorUtil.component(getPrefix() + success));

        gui.refresh();
    }

    private void handleCollect(Player player, Reactor reactor, ReactorGUI gui) {
        double stored = reactor.getStoredBalance();
        if (stored <= 0.0) {
            player.sendMessage(ColorUtil.component(getPrefix() + plugin.getConfigManager().getMessage("no-funds-to-collect")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        reactor.withdrawAllStored();
        economyService.deposit(player, stored);
        reactorManager.saveReactors();
        hologramService.updateHologram(reactor);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        String msg = plugin.getConfigManager().getMessage("funds-collected")
                .replace("%amount%", String.format("%.1f", stored))
                .replace("%currency%", plugin.getConfigManager().getCurrencySymbol());
        player.sendMessage(ColorUtil.component(getPrefix() + msg));

        gui.refresh();
    }

    private void handleToggle(Player player, Reactor reactor, ReactorGUI gui) {
        boolean newState = !reactor.isActive();
        reactor.setActive(newState);
        reactorManager.saveReactors();
        hologramService.updateHologram(reactor);

        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, newState ? 1.5f : 0.8f);
        String msgKey = newState ? "toggled-on" : "toggled-off";
        player.sendMessage(ColorUtil.component(getPrefix() + plugin.getConfigManager().getMessage(msgKey)));

        gui.refresh();
    }
}
