package com.ejyqyl.atomduels.bot;

import com.ejyqyl.atomduels.kit.Kit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active combat bots and custom training NPC bots.
 * Authored by ejyqyl (https://github.com/aqkooo).
 */
public class BotManager {

    private final Plugin plugin;
    private final Map<UUID, DuelBot> activeBots = new ConcurrentHashMap<>();
    private final Map<UUID, DuelBot> trainingBots = new ConcurrentHashMap<>();
    private final Map<UUID, BotSettings> playerSettings = new ConcurrentHashMap<>();

    public BotManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public BotSettings getOrCreateSettings(UUID playerUuid) {
        return playerSettings.computeIfAbsent(playerUuid, k -> new BotSettings());
    }

    public DuelBot spawnBot(Player opponent, Location spawnLocation, Kit kit, BotDifficulty difficulty) {
        BotProfile profile = new BotProfile(difficulty);
        DuelBot bot = new DuelBot(plugin, profile, opponent, spawnLocation, kit);
        bot.spawn();
        activeBots.put(bot.getBotId(), bot);
        return bot;
    }

    public DuelBot spawnTrainingBot(Player player, BotSettings settings) {
        removeTrainingBot(player.getUniqueId());

        Location loc = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(2.0));
        loc.setYaw((player.getLocation().getYaw() + 180.0f) % 360.0f);
        loc.setPitch(0.0f);

        DuelBot bot = new DuelBot(plugin, settings, player, loc);
        bot.spawn();
        trainingBots.put(player.getUniqueId(), bot);
        return bot;
    }

    public boolean removeTrainingBot(UUID playerUuid) {
        DuelBot bot = trainingBots.remove(playerUuid);
        if (bot != null) {
            bot.remove();
            return true;
        }
        return false;
    }

    public DuelBot getTrainingBot(UUID playerUuid) {
        return trainingBots.get(playerUuid);
    }

    public DuelBot getBot(UUID botId) {
        DuelBot b = activeBots.get(botId);
        if (b != null) return b;
        for (DuelBot tb : trainingBots.values()) {
            if (tb.getBotId().equals(botId)) return tb;
        }
        return null;
    }

    public DuelBot getBotByEntity(Entity entity) {
        if (entity == null) return null;
        for (DuelBot bot : activeBots.values()) {
            if (bot.getEntity() != null && bot.getEntity().getUniqueId().equals(entity.getUniqueId())) {
                return bot;
            }
        }
        for (DuelBot bot : trainingBots.values()) {
            if (bot.getEntity() != null && bot.getEntity().getUniqueId().equals(entity.getUniqueId())) {
                return bot;
            }
        }
        return null;
    }

    public void removeBot(UUID botId) {
        DuelBot bot = activeBots.remove(botId);
        if (bot != null) {
            bot.remove();
        }
    }

    public void cleanupAll() {
        for (DuelBot bot : activeBots.values()) {
            bot.remove();
        }
        activeBots.clear();

        for (DuelBot bot : trainingBots.values()) {
            bot.remove();
        }
        trainingBots.clear();
    }
}
