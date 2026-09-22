package ru.atomicsqd.atommessage.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import ru.atomicsqd.atommessage.AtomMessage;

public class PlayerListener implements Listener {
    private final AtomMessage plugin;

    public PlayerListener(@NotNull AtomMessage plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getConfigManager().isDirectTabListEnabled()) {
            // Delay 5 ticks to allow player fully load and client initialize
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (event.getPlayer().isOnline()) {
                    plugin.getTabMessageManager().sendDirectTab(event.getPlayer());
                }
            }, 5L);
        }
    }
}
