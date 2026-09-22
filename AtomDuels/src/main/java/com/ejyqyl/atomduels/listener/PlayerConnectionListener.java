package com.ejyqyl.atomduels.listener;

import com.ejyqyl.atomduels.bot.BotManager;
import com.ejyqyl.atomduels.config.MessageManager;
import com.ejyqyl.atomduels.data.PlayerData;
import com.ejyqyl.atomduels.data.StatsManager;
import com.ejyqyl.atomduels.duel.DuelManager;
import com.ejyqyl.atomduels.queue.QueueManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player join caching, claim notifications, bot cleanups, and combat-log quit penalties.
 * Authored by ejyqyl (https://github.com/aqkooo).
 */
public class PlayerConnectionListener implements Listener {

    private final StatsManager statsManager;
    private final DuelManager duelManager;
    private final QueueManager queueManager;
    private final MessageManager messageManager;
    private final BotManager botManager;

    public PlayerConnectionListener(StatsManager statsManager, DuelManager duelManager,
                                    QueueManager queueManager, MessageManager messageManager,
                                    BotManager botManager) {
        this.statsManager = statsManager;
        this.duelManager = duelManager;
        this.queueManager = queueManager;
        this.messageManager = messageManager;
        this.botManager = botManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = statsManager.getPlayerData(player);

        if (!data.getClaimItems().isEmpty()) {
            messageManager.sendRawMessage(player, "{prefix} &eУ вас есть сохранённые предметы с дуэлей! Заберите их через &6/duels claim&e.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (duelManager.isInDuel(player.getUniqueId())) {
            duelManager.handleQuit(player);
        }

        if (botManager != null) {
            botManager.removeTrainingBot(player.getUniqueId());
        }

        queueManager.removeFromQueue(player.getUniqueId());
        statsManager.unloadPlayer(player.getUniqueId());
    }
}
