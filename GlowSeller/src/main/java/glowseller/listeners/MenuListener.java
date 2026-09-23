package glowseller.listeners;

import glowseller.Main;
import glowseller.menu.AbstractMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MenuListener implements Listener {
    private final Main plugin;
    private final Map<UUID, Long> clickDebounce = new ConcurrentHashMap<>();

    public MenuListener(Main plugin) {
        this.plugin = plugin;
    }

    public MenuListener() {
        this(Main.getInstance());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof AbstractMenu menu)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // 1. Non-interactive menus (standard showcase/category GUIs)
        if (!menu.isInteractive()) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);

            if (!event.isLeftClick() && !event.isRightClick()) {
                player.updateInventory();
                return;
            }

            if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR || event.isShiftClick()
                    || event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.SWAP_OFFHAND
                    || event.getHotbarButton() != -1) {
                player.updateInventory();
                return;
            }

            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) {
                return;
            }

            long now = System.currentTimeMillis();
            Long lastClick = clickDebounce.get(player.getUniqueId());
            if (lastClick != null && (now - lastClick) < 150) {
                return;
            }
            clickDebounce.put(player.getUniqueId(), now);

            menu.handleClick(event);
            return;
        }

        // 2. Interactive menus (e.g., SellMenu drop-seller)
        int rawSlot = event.getRawSlot();
        int topSize = top.getSize();

        // Click outside the window
        if (rawSlot < 0) {
            scheduleChange(menu);
            return;
        }

        // Prevent COLLECT_TO_CURSOR universally in interactive menus to avoid vacuuming control items
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            player.updateInventory();
            return;
        }

        // Click inside the top inventory
        if (rawSlot < topSize) {
            if (menu.isSlotInteractive(rawSlot)) {
                // Allowed drop slot (0-44)
                event.setCancelled(false);
                scheduleChange(menu);
            } else {
                // Restricted control slot (45-53)
                event.setCancelled(true);
                event.setResult(Event.Result.DENY);

                // Any non-left/right click, shift-click, number key, offhand swap, or hotbar button on restricted slots is completely blocked!
                if (!event.isLeftClick() && !event.isRightClick()) {
                    player.updateInventory();
                    return;
                }
                if (event.isShiftClick() || event.getClick() == ClickType.NUMBER_KEY
                        || event.getClick() == ClickType.SWAP_OFFHAND || event.getHotbarButton() != -1) {
                    player.updateInventory();
                    return;
                }

                long now = System.currentTimeMillis();
                Long lastClick = clickDebounce.get(player.getUniqueId());
                if (lastClick == null || (now - lastClick) >= 150) {
                    clickDebounce.put(player.getUniqueId(), now);
                    menu.handleClick(event);
                }
            }
            return;
        }

        // Click inside the bottom (player's) inventory
        if (event.isShiftClick()) {
            // Forward shift-click to menu to place safely into drop slots without overflowing
            menu.handleClick(event);
            scheduleChange(menu);
            return;
        }

        // Regular click inside player inventory
        event.setCancelled(false);
        scheduleChange(menu);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof AbstractMenu menu)) {
            return;
        }

        if (!menu.isInteractive()) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < topSize && !menu.isSlotInteractive(rawSlot)) {
                event.setCancelled(true);
                event.setResult(Event.Result.DENY);
                return;
            }
        }

        menu.handleDrag(event);
        scheduleChange(menu);
    }

    private void scheduleChange(AbstractMenu menu) {
        Main p = plugin != null ? plugin : Main.getInstance();
        if (p != null && p.isEnabled()) {
            Bukkit.getScheduler().runTask(p, menu::onContentsChanged);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof AbstractMenu menu) {
            menu.handleClose(event);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clickDebounce.remove(event.getPlayer().getUniqueId());
    }
}
