package me.ejyqyl.tituls.manager;

import me.ejyqyl.tituls.AtomTitulsPlugin;
import me.ejyqyl.tituls.sorting.SortDirection;
import me.ejyqyl.tituls.sorting.SortMode;
import me.ejyqyl.tituls.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {
    private final AtomTitulsPlugin plugin;

    private FileConfiguration databaseConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration titulsConfig;
    private FileConfiguration menuConfig;
    private FileConfiguration tagConfig;
    private FileConfiguration sortingConfig;

    private final List<SortMode> sortModes = new ArrayList<>();

    public ConfigManager(@NotNull AtomTitulsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        this.databaseConfig = loadConfigFile("database.yml");
        this.messagesConfig = loadConfigFile("messages.yml");
        this.titulsConfig = loadConfigFile("tituls.yml");
        this.menuConfig = loadConfigFile("menu/tituls.yml");
        this.tagConfig = loadConfigFile("titultag.yml");
        this.sortingConfig = loadConfigFile("sorting.yml");

        loadSortModes();
    }

    private FileConfiguration loadConfigFile(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                plugin.saveResource(resourcePath, false);
            } catch (IllegalArgumentException e) {
                // Not in jar
                try {
                    file.createNewFile();
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to create " + resourcePath, ex);
                }
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));
            }
        } catch (Exception ignored) {
        }

        return config;
    }

    private void loadSortModes() {
        sortModes.clear();
        ConfigurationSection section = sortingConfig.getConfigurationSection("sorting");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String name = section.getString(key + ".name", key);
                SortDirection dir = SortDirection.fromString(section.getString(key + ".type", "MAX"));
                String value = section.getString(key + ".value", "");
                String selected = section.getString(key + ".selected-name", "");
                String unselected = section.getString(key + ".unselected-name", "");
                int priority = section.getInt(key + ".priority", 0);

                sortModes.add(new SortMode(name, dir, value, selected, unselected, priority));
            }
        }
        sortModes.sort(Comparator.comparingInt(SortMode::getPriority));
    }

    @NotNull
    public List<SortMode> getSortModes() {
        return sortModes;
    }

    @Nullable
    public SortMode getDefaultSortMode() {
        return sortModes.isEmpty() ? null : sortModes.get(0);
    }

    @Nullable
    public SortMode getSortModeByName(String name) {
        if (name == null) return getDefaultSortMode();
        for (SortMode mode : sortModes) {
            if (mode.getName().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return getDefaultSortMode();
    }

    @Nullable
    public SortMode getNextSortMode(String currentName) {
        if (sortModes.isEmpty()) return null;
        int index = 0;
        for (int i = 0; i < sortModes.size(); i++) {
            if (sortModes.get(i).getName().equalsIgnoreCase(currentName)) {
                index = i;
                break;
            }
        }
        int next = (index + 1) % sortModes.size();
        return sortModes.get(next);
    }

    // Messages
    public String getMessage(String path) {
        return messagesConfig.getString("messages." + path, "&cMessage not found: " + path);
    }

    public String getFormattedMessage(String path) {
        return TextUtil.toLegacy(getMessage(path));
    }

    // Config getters
    public FileConfiguration getDatabaseConfig() {
        return databaseConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getTitulsConfig() {
        return titulsConfig;
    }

    public FileConfiguration getMenuConfig() {
        return menuConfig;
    }

    public FileConfiguration getTagConfig() {
        return tagConfig;
    }

    public FileConfiguration getSortingConfig() {
        return sortingConfig;
    }

    // Tag Config Helpers
    public Material getTagMaterial() {
        String matStr = tagConfig.getString("item.material", "NAME_TAG");
        try {
            return Material.valueOf(matStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.NAME_TAG;
        }
    }

    public String getTagName() {
        return tagConfig.getString("item.name", "{titul}");
    }

    public List<String> getTagLore() {
        return tagConfig.getStringList("item.lore");
    }

    public String getTagYesTab() {
        return tagConfig.getString("placeholders.yes.tab", "{titul}");
    }

    public String getTagYesBoard() {
        return tagConfig.getString("placeholders.yes.board", "{titul}");
    }

    public String getTagNoTab() {
        return tagConfig.getString("placeholders.no.tab", "");
    }

    public String getTagNoBoard() {
        return tagConfig.getString("placeholders.no.board", "&7Нет");
    }
}
