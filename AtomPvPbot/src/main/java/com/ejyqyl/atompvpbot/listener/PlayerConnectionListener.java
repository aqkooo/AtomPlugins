package com.ejyqyl.atompvpbot.listener;

import com.ejyqyl.atompvpbot.data.StatsManager;
import com.ejyqyl.atompvpbot.fight.FightManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player session persistence and disconnect cleanup.
 *
 * @author ejyqyl
 */
public class PlayerConnectionListener implements Listener {

    private final FightManager fightManager;
    private final StatsManager statsManager;

    public PlayerConnectionListener(FightManager fightManager, StatsManager statsManager) {
        this.fightManager = fightManager;
        this.statsManager = statsManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        statsManager.getPlayerData(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (fightManager.isInFight(player)) {
            fightManager.stopFight(player, false);
        }
        statsManager.unloadPlayer(player.getUniqueId());
    }
}
