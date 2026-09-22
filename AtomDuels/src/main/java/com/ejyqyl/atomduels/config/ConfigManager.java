package com.ejyqyl.atomduels.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Accessor for configuration parameters.
 * Authored by ejyqyl.
 */
public class ConfigManager {

    private final Plugin plugin;
    private final Set<String> allowedCommands = new HashSet<>();
    private double feePercent;
    private double minBet;
    private double maxBet;
    private String crashResolution;
    private int countdownSeconds;
    private int roundTimeSeconds;
    private int openChallengeExpireSeconds;
    private int defaultRating;
    private int calibrationGames;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.feePercent = config.getDouble("economy.fee-percent", 5.0);
        this.minBet = config.getDouble("economy.min-bet", 100.0);
        this.maxBet = config.getDouble("economy.max-bet", 10000000.0);
        this.crashResolution = config.getString("crash-resolution", "DRAW").toUpperCase();
        this.countdownSeconds = config.getInt("match.countdown", 5);
        this.roundTimeSeconds = config.getInt("match.round-time", 300);
        this.openChallengeExpireSeconds = config.getInt("match.open-challenge-expire", 120);
        this.defaultRating = config.getInt("elo.default-rating", 1000);
        this.calibrationGames = config.getInt("elo.calibration-games", 10);

        this.allowedCommands.clear();
        List<String> list = config.getStringList("allowed-commands");
        for (String cmd : list) {
            this.allowedCommands.add(cmd.toLowerCase());
        }
    }

    public double getFeePercent() {
        return feePercent;
    }

    public double getMinBet() {
        return minBet;
    }

    public double getMaxBet() {
        return maxBet;
    }

    public String getCrashResolution() {
        return crashResolution;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public int getRoundTimeSeconds() {
        return roundTimeSeconds;
    }

    public int getOpenChallengeExpireSeconds() {
        return openChallengeExpireSeconds;
    }

    public int getDefaultRating() {
        return defaultRating;
    }

    public int getCalibrationGames() {
        return calibrationGames;
    }

    public boolean isCommandAllowed(String command) {
        if (command == null) return false;
        String base = command.toLowerCase().trim();
        if (!base.startsWith("/")) base = "/" + base;
        String[] parts = base.split(" ");
        return allowedCommands.contains(parts[0]);
    }
}
