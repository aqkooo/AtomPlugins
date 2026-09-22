package com.ejyqyl.atombot.listener;

import com.ejyqyl.atombot.manager.BotManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Automatically removes spawned training bots when players disconnect.
 *
 * @author ejyqyl
 */
public class PlayerQuitListener implements Listener {

    private final BotManager botManager;

    public PlayerQuitListener(BotManager botManager) {
        this.botManager = botManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        botManager.removeBot(player.getUniqueId());
    }
}
