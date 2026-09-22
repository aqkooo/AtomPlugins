package ru.atomicsqd.atommessage.config;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.atomicsqd.atommessage.model.RotationOrder;
import ru.atomicsqd.atommessage.model.TabMessage;
import ru.atomicsqd.atommessage.util.ColorUtil;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {
    private final JavaPlugin plugin;

    private FileConfiguration config;
    private FileConfiguration messages;

    private int defaultInterval;
    private RotationOrder rotationOrder;
    private boolean directTabListEnabled;
    private long directTabListUpdateTicks;
    private List<String> directHeaderLines = new ArrayList<>();
    private List<String> directFooterLines = new ArrayList<>();
    private String globalSound;
    private float soundVolume;
    private float soundPitch;

    private final List<TabMessage> tabMessages = new ArrayList<>();

    public ConfigManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.config = loadConfigFile("config.yml");
        this.messages = loadConfigFile("messages.yml");

        this.defaultInterval = Math.max(1, config.getInt("settings.interval", 15));
        this.rotationOrder = RotationOrder.fromString(config.getString("settings.rotation-order", "SEQUENTIAL"));

        this.directTabListEnabled = config.getBoolean("settings.direct-tablist.enabled", false);
        this.directTabListUpdateTicks = Math.max(1, config.getLong("settings.direct-tablist.update-ticks", 20));
        this.directHeaderLines = config.getStringList("settings.direct-tablist.header");
        this.directFooterLines = config.getStringList("settings.direct-tablist.footer");

        this.globalSound = config.getString("settings.global-sound", "NONE");
        this.soundVolume = (float) config.getDouble("settings.sound-volume", 0.5);
        this.soundPitch = (float) config.getDouble("settings.sound-pitch", 1.0);

        loadMessages();
    }

    private void loadMessages() {
        tabMessages.clear();
        ConfigurationSection section = config.getConfigurationSection("messages");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection msgSec = section.getConfigurationSection(key);
            if (msgSec == null) {
                // If it's a simple string or string list directly under key
                if (section.isList(key)) {
                    tabMessages.add(new TabMessage(key, section.getStringList(key), defaultInterval, null, null));
                } else if (section.isString(key)) {
                    tabMessages.add(new TabMessage(key, Collections.singletonList(section.getString(key)), defaultInterval, null, null));
                }
                continue;
            }

            int duration = msgSec.getInt("duration", defaultInterval);
            if (duration <= 0) duration = defaultInterval;

            List<String> lines = msgSec.getStringList("lines");
            if (lines.isEmpty() && msgSec.isString("text")) {
                lines = Collections.singletonList(msgSec.getString("text"));
            }

            String permission = msgSec.getString("permission", null);
            String sound = msgSec.getString("sound", null);

            tabMessages.add(new TabMessage(key, lines, duration, permission, sound));
        }

        plugin.getLogger().info("Loaded " + tabMessages.size() + " tab messages from config.yml");
    }

    private FileConfiguration loadConfigFile(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                plugin.saveResource(name, false);
            } catch (Exception ex) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to create " + name, e);
                }
            }
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        try (InputStream stream = plugin.getResource(name)) {
            if (stream != null) {
                cfg.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));
            }
        } catch (Exception ignored) {
        }
        return cfg;
    }

    @NotNull
    public Component getMessageComponent(@NotNull String path, @NotNull String... replacements) {
        String raw = messages.getString(path, "&cMessage not found: " + path);
        String prefix = messages.getString("prefix", "");
        String full = prefix + raw;

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                full = full.replace(replacements[i], replacements[i + 1]);
            }
        }

        return ColorUtil.parseComponent(full, null);
    }

    @NotNull
    public List<Component> getHelpList() {
        List<String> list = messages.getStringList("help");
        List<Component> result = new ArrayList<>();
        for (String line : list) {
            result.add(ColorUtil.parseComponent(line, null));
        }
        return result;
    }

    public int getDefaultInterval() {
        return defaultInterval;
    }

    public RotationOrder getRotationOrder() {
        return rotationOrder;
    }

    public boolean isDirectTabListEnabled() {
        return directTabListEnabled;
    }

    public long getDirectTabListUpdateTicks() {
        return directTabListUpdateTicks;
    }

    public List<String> getDirectHeaderLines() {
        return directHeaderLines;
    }

    public List<String> getDirectFooterLines() {
        return directFooterLines;
    }

    public String getGlobalSound() {
        return globalSound;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    @NotNull
    public List<TabMessage> getTabMessages() {
        return Collections.unmodifiableList(tabMessages);
    }
}
