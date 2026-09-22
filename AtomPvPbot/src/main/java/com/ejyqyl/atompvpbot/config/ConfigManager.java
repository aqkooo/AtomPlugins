package com.ejyqyl.atompvpbot.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages plugin configuration, gameplay parameters, and security whitelists.
 *
 * @author ejyqyl
 */
public class ConfigManager {

    private final Plugin plugin;
    private FileConfiguration config;
    private final List<String> allowedCommands = new ArrayList<>();

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        allowedCommands.clear();
        for (String cmd : config.getStringList("allowed-commands")) {
            allowedCommands.add(cmd.toLowerCase().trim());
        }
    }

    public boolean isCommandAllowed(String message) {
        if (message == null || message.isEmpty()) return false;
        String label = message.split(" ")[0].toLowerCase();
        for (String allowed : allowedCommands) {
            if (label.equalsIgnoreCase(allowed)) return true;
        }
        return false;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public double getCustomMin(String path, double def) {
        return config.getDouble("custom-ai-bounds." + path + ".min", def);
    }

    public double getCustomMax(String path, double def) {
        return config.getDouble("custom-ai-bounds." + path + ".max", def);
    }

    public double getCustomDefault(String path, double def) {
        return config.getDouble("custom-ai-bounds." + path + ".default", def);
    }
}
