package ru.glowdev.glowsnake.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.glowdev.glowsnake.GlowSnakePlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ScoreManager {

    private final GlowSnakePlugin plugin;
    private final File file;
    private FileConfiguration config;

    public ScoreManager(GlowSnakePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "scores.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create scores.yml", e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save scores.yml", e);
        }
    }

    public int getHighScore(UUID uuid) {
        if (uuid == null) return 0;
        return config.getInt("scores." + uuid + ".high-score", 0);
    }

    /**
     * Updates the score for a player.
     *
     * @param uuid player UUID
     * @param name player Name
     * @param score the score achieved in the session
     * @return true if this is a new high record (score > old record), false otherwise
     */
    public synchronized boolean updateScore(UUID uuid, String name, int score) {
        if (uuid == null) return false;

        String path = "scores." + uuid;
        int oldHighScore = config.getInt(path + ".high-score", 0);
        int games = config.getInt(path + ".games-played", 0);

        config.set(path + ".name", name != null ? name : "Unknown");
        config.set(path + ".games-played", games + 1);
        config.set(path + ".last-played", System.currentTimeMillis());

        boolean isNewRecord = false;
        if (score > oldHighScore) {
            config.set(path + ".high-score", score);
            isNewRecord = true;
        }

        save();
        return isNewRecord;
    }

    public List<ScoreEntry> getTopScores(int limit) {
        List<ScoreEntry> entries = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("scores");
        if (section == null) {
            return entries;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String name = section.getString(key + ".name", "Unknown");
                int highScore = section.getInt(key + ".high-score", 0);
                int games = section.getInt(key + ".games-played", 0);
                entries.add(new ScoreEntry(uuid, name, highScore, games));
            } catch (Exception ignored) {
            }
        }

        entries.sort((a, b) -> Integer.compare(b.getHighScore(), a.getHighScore()));
        if (entries.size() > limit) {
            return entries.subList(0, limit);
        }
        return entries;
    }

    public static class ScoreEntry {
        private final UUID uuid;
        private final String name;
        private final int highScore;
        private final int gamesPlayed;

        public ScoreEntry(UUID uuid, String name, int highScore, int gamesPlayed) {
            this.uuid = uuid;
            this.name = name;
            this.highScore = highScore;
            this.gamesPlayed = gamesPlayed;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public int getHighScore() {
            return highScore;
        }

        public int getGamesPlayed() {
            return gamesPlayed;
        }
    }
}
