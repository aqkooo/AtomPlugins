package com.ejyqyl.atombot.manager;

import com.ejyqyl.atombot.entity.TrainingBot;
import com.ejyqyl.atombot.model.BotSettings;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player training bots and their custom parameters.
 *
 * @author ejyqyl
 */
public class BotManager {

    private final Plugin plugin;
    private final Map<UUID, TrainingBot> activeBots = new ConcurrentHashMap<>();
    private final Map<UUID, BotSettings> playerSettings = new ConcurrentHashMap<>();

    public BotManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public BotSettings getOrCreateSettings(UUID playerUuid) {
        return playerSettings.computeIfAbsent(playerUuid, k -> new BotSettings());
    }

    public TrainingBot spawnBot(Player player) {
        removeBot(player.getUniqueId());

        BotSettings settings = getOrCreateSettings(player.getUniqueId());
        Location loc = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(2.0));
        loc.setYaw((player.getLocation().getYaw() + 180.0f) % 360.0f);
        loc.setPitch(0.0f);

        TrainingBot bot = new TrainingBot(plugin, settings, player, loc);
        bot.spawn();
        activeBots.put(player.getUniqueId(), bot);
        return bot;
    }

    public boolean removeBot(UUID playerUuid) {
        TrainingBot bot = activeBots.remove(playerUuid);
        if (bot != null) {
            bot.remove();
            return true;
        }
        return false;
    }

    public TrainingBot getBot(UUID playerUuid) {
        return activeBots.get(playerUuid);
    }

    public TrainingBot getBotByEntity(Entity entity) {
        if (entity == null) return null;
        for (TrainingBot bot : activeBots.values()) {
            if (bot.getEntity() != null && bot.getEntity().getUniqueId().equals(entity.getUniqueId())) {
                return bot;
            }
        }
        return null;
    }

    public void cleanupAll() {
        for (TrainingBot bot : activeBots.values()) {
            bot.remove();
        }
        activeBots.clear();
    }
}
