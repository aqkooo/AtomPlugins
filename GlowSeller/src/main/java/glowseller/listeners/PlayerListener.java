package glowseller.listeners;

import glowseller.Main;
import glowseller.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import glowseller.menu.AbstractMenu;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class PlayerListener implements Listener {
    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuitMenuClose(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof AbstractMenu) {
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof AbstractMenu) {
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        plugin.getPlayerDataCache().markLoading(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = plugin.getPlayerRepository().load(uuid);
            if (!player.isOnline()) {
                plugin.getPlayerDataCache().unmarkLoading(uuid);
                return;
            }
            plugin.getPlayerDataCache().mergeAndPut(uuid, data);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PlayerData data = plugin.getPlayerDataCache().remove(uuid);
        if (data != null) {
            if (data.getAutoSellMode() == PlayerData.AutoSellMode.ENABLED_SESSION) {
                data.setAutoSellMode(PlayerData.AutoSellMode.OFF);
            }
            if (data.isDirty()) {
                if (plugin.isEnabled()) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        plugin.getPlayerRepository().save(data);
                    });
                } else {
                    plugin.getPlayerRepository().save(data);
                }
            }
        }
    }
}
