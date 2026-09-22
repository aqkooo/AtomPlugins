package ru.glowdevv.glowcustomloot.config;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.glowdevv.glowcustomloot.model.LootMode;
import ru.glowdevv.glowcustomloot.util.TextUtil;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {
    private final JavaPlugin plugin;

    private FileConfiguration config;
    private FileConfiguration menus;
    private FileConfiguration messages;

    private boolean autoSaveOnClose;
    private LootMode defaultLootMode;
    private int maxItemsPerChest;
    private double minChance;

    public ConfigManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.config = loadYaml("config.yml");
        this.menus = loadYaml("menus.yml");
        this.messages = loadYaml("messages.yml");

        this.autoSaveOnClose = config.getBoolean("settings.auto_save_on_close", true);
        this.defaultLootMode = LootMode.fromString(config.getString("loot_generation.default_mode", "MERGE"));
        this.maxItemsPerChest = Math.max(1, config.getInt("loot_generation.max_items_per_chest", 27));
        this.minChance = config.getDouble("loot_generation.min_chance", 0.1);
    }

    public void loadAll() {
        load();
    }

    public void reload() {
        load();
    }

    @NotNull
    public String getPrefix() {
        return messages != null ? messages.getString("prefix", "") : "";
    }

    @NotNull
    public String getRawMessage(@NotNull String path) {
        return messages != null ? messages.getString(path, "") : "";
    }

    private FileConfiguration loadYaml(String filename) {
        File file = new File(plugin.getDataFolder(), filename);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                plugin.saveResource(filename, false);
            } catch (Exception ex) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to create " + filename, e);
                }
            }
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        try (InputStream stream = plugin.getResource(filename)) {
            if (stream != null) {
                cfg.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));
            }
        } catch (Exception ignored) {
        }
        return cfg;
    }

    @NotNull
    public Component getMessage(@NotNull String path, @NotNull String... replacements) {
        String raw = messages.getString(path, "&cMessage not found: " + path);
        String prefix = messages.getString("prefix", "");
        String full = prefix + raw;

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                full = full.replace(replacements[i], replacements[i + 1]);
            }
        }
        return TextUtil.parse(full, false);
    }

    @NotNull
    public List<Component> getHelpList() {
        List<String> raw = messages.getStringList("help");
        List<Component> list = new ArrayList<>();
        for (String line : raw) {
            list.add(TextUtil.parse(line, false));
        }
        return list;
    }

    public boolean isAutoSaveOnClose() {
        return autoSaveOnClose;
    }

    public LootMode getDefaultLootMode() {
        return defaultLootMode;
    }

    public int getMaxItemsPerChest() {
        return maxItemsPerChest;
    }

    public double getMinChance() {
        return minChance;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMenus() {
        return menus;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public boolean isDebug() {
        return config != null && config.getBoolean("settings.debug_mode", false);
    }
}
