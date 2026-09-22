package com.ejyqyl.atompvpbot.fight;

import com.ejyqyl.atompvpbot.ai.BotBehaviorMode;
import com.ejyqyl.atompvpbot.ai.BotDifficulty;
import com.ejyqyl.atompvpbot.arena.Arena;
import com.ejyqyl.atompvpbot.data.StatsManager;
import com.ejyqyl.atompvpbot.entity.PvPBotEntity;
import com.ejyqyl.atompvpbot.kit.BotKit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active fight sessions and ticks running bots.
 *
 * @author ejyqyl
 */
public class FightManager {

    private final Plugin plugin;
    private final StatsManager statsManager;
    private final Map<UUID, BotFight> activeFights = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public FightManager(Plugin plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        startTicker();
    }

    private void startTicker() {
        this.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (BotFight fight : activeFights.values()) {
                if (fight.getState() == BotFight.State.FIGHTING || fight.getState() == BotFight.State.COUNTDOWN) {
                    PvPBotEntity bot = fight.getBot();
                    if (bot != null) {
                        bot.tick();
                    }
                }
            }
        }, 1L, 1L);
    }

    public BotFight startFight(Player player, BotDifficulty difficulty, BotBehaviorMode behaviorMode,
                               BotKit kit, Arena arena, boolean continuous) {
        if (isInFight(player)) {
            stopFight(player, false);
        }

        PvPBotEntity bot = new PvPBotEntity(plugin);
        bot.setDifficulty(difficulty);
        bot.setBehaviorMode(behaviorMode);
        bot.setKit(kit);
        bot.setInfiniteTotemMode(continuous);

        BotFight fight = new BotFight(plugin, player, bot, arena, kit);
        fight.setContinuousTraining(continuous);
        activeFights.put(player.getUniqueId(), fight);

        fight.start();
        return fight;
    }

    public void stopFight(Player player, boolean playerWon) {
        if (player == null) return;
        BotFight fight = activeFights.remove(player.getUniqueId());
        if (fight != null) {
            fight.endFight(playerWon);
            if (statsManager != null) {
                if (playerWon) {
                    statsManager.getPlayerData(player).addWin();
                } else {
                    statsManager.getPlayerData(player).addLoss();
                }
                statsManager.savePlayerData(player.getUniqueId());
            }
        }
    }

    public BotFight getFight(Player player) {
        if (player == null) return null;
        return activeFights.get(player.getUniqueId());
    }

    public boolean isInFight(Player player) {
        if (player == null) return false;
        return activeFights.containsKey(player.getUniqueId());
    }

    public BotFight getFightByBot(Entity entity) {
        if (entity == null) return null;
        for (BotFight fight : activeFights.values()) {
            if (fight.getBot() != null && fight.getBot().getMob() != null &&
                    fight.getBot().getMob().getUniqueId().equals(entity.getUniqueId())) {
                return fight;
            }
        }
        return null;
    }

    public void stopAllFights() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        for (BotFight fight : activeFights.values()) {
            fight.endFight(false);
        }
        activeFights.clear();
    }

    public Collection<BotFight> getActiveFights() {
        return Collections.unmodifiableCollection(activeFights.values());
    }
}
