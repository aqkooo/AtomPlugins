package ru.glowdev.glowsnake.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import ru.glowdev.glowsnake.GlowSnakePlugin;
import ru.glowdev.glowsnake.config.ConfigManager;
import ru.glowdev.glowsnake.game.SnakeGameSession;
import ru.glowdev.glowsnake.util.SoundUtil;

public class InventoryListener implements Listener {

    private final GlowSnakePlugin plugin;

    public InventoryListener(GlowSnakePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        SnakeGameSession session = plugin.getGameManager().getSession(player);
        if (session == null) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        if (!topInventory.equals(session.getInventory())) {
            return;
        }

        // Always cancel item dragging/taking inside the Snake game GUI
        event.setCancelled(true);

        // Handle hotbar number keys (1-9)
        if (event.getClick() == ClickType.NUMBER_KEY) {
            session.handleNumberKeyClick(event.getHotbarButton());
            return;
        }

        int rawSlot = event.getRawSlot();
        ConfigManager cfg = plugin.getConfigManager();

        if (session.isGameOver() || session.isVictory()) {
            if (rawSlot == SnakeGameSession.GAMEOVER_RESTART) {
                SoundUtil.playSound(player, cfg.getSoundClick(), 1.0f, 1.0f);
                session.restart();
            } else if (rawSlot == SnakeGameSession.GAMEOVER_EXIT) {
                SoundUtil.playSound(player, cfg.getSoundClick(), 1.0f, 1.0f);
                player.closeInventory();
            }
        } else {
            // Directional click support (works on top GUI and player inventory)
            if (cfg.isEnableGuiClicks()) {
                session.handleDirectionClick(rawSlot);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        SnakeGameSession session = plugin.getGameManager().getSession(player);
        if (session == null) {
            return;
        }

        if (event.getInventory().equals(session.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        SnakeGameSession session = plugin.getGameManager().getSession(player);
        if (session != null && event.getInventory().equals(session.getInventory())) {
            // Clean up session and cancel game task immediately
            plugin.getGameManager().stopGame(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameManager().hasSession(player)) {
            plugin.getGameManager().stopGame(player);
        }
    }
}
