package glowseller.configs;

import glowseller.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public abstract class CustomConfig {
    protected final Main plugin;
    protected final String fileName;
    protected File file;
    protected FileConfiguration config;

    public CustomConfig(Main plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            }
        }

        config = YamlConfiguration.loadConfiguration(file);

        // Set defaults from embedded resource
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            Reader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8);
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
            config.setDefaults(defConfig);
        }

        try {
            parse();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to parse configuration: " + fileName, e);
        }
    }

    public void save() {
        if (config == null || file == null) return;
        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save configuration: " + fileName, e);
        }
    }

    public abstract void parse();

    public FileConfiguration getConfig() {
        return config;
    }

    public String getFileName() {
        return fileName;
    }
}
