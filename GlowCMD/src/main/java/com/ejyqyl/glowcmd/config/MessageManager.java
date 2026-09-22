package com.ejyqyl.glowcmd.config;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.util.ColorUtil;
import com.ejyqyl.glowcmd.util.PlaceholderUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance localized message manager.
 * Caches all messages in memory, supports non-destructive YAML auto-recovery,
 * placeholder resolution, hex color formatting, and silent suppression of empty messages.
 *
 * @author ejyqyl
 */
public final class MessageManager {

    private final GlowCMD plugin;
    private final File messagesFile;
    private final Map<String, String> messagesCache = new ConcurrentHashMap<>();
    private final Map<String, String> defaultCache = new ConcurrentHashMap<>();
    private String prefix = "&8[&bGlowCMD&8]";

    public MessageManager(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        reload();
    }

    /**
     * Reloads messages.yml from disk, recovers any missing keys from internal defaults,
     * and refreshes the in-memory cache.
     */
    public synchronized void reload() {
        messagesCache.clear();
        defaultCache.clear();

        // 1. Ensure file exists on disk
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        // 2. Load user configuration
        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // 3. Load default embedded configuration
        YamlConfiguration defaultConfig = null;
        try (InputStream stream = plugin.getResource("messages.yml")) {
            if (stream != null) {
                defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
                for (String key : defaultConfig.getKeys(true)) {
                    if (defaultConfig.isString(key)) {
                        defaultCache.put(key, defaultConfig.getString(key, ""));
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read embedded default messages.yml: " + e.getMessage());
        }

        // 4. Non-destructive auto-recovery of missing keys
        if (defaultConfig != null) {
            boolean updated = false;
            for (String key : defaultConfig.getKeys(true)) {
                if (!defaultConfig.isConfigurationSection(key) && !userConfig.contains(key)) {
                    userConfig.set(key, defaultConfig.get(key));
                    updated = true;
                }
            }
            if (updated) {
                try {
                    userConfig.save(messagesFile);
                    plugin.getLogger().info("Repaired messages.yml: added missing default keys without overwriting existing entries.");
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to save repaired messages.yml: " + e.getMessage());
                }
            }
        }

        // 5. Populate in-memory cache
        for (String key : userConfig.getKeys(true)) {
            if (userConfig.isString(key)) {
                messagesCache.put(key, userConfig.getString(key, ""));
            }
        }

        // 6. Cache prefix
        this.prefix = messagesCache.getOrDefault("prefix", defaultCache.getOrDefault("prefix", "&8[&bGlowCMD&8]"));
    }

    /**
     * Sends a localized message to the target sender.
     * If the message is configured as an empty string (""), nothing is sent.
     *
     * @param sender Target recipient (Player or Console)
     * @param key    Configuration message key
     */
    public void send(@Nullable CommandSender sender, @NotNull String key) {
        send(sender, key, null);
    }

    /**
     * Sends a localized message with custom placeholders to the target sender.
     *
     * @param sender              Target recipient
     * @param key                 Configuration message key
     * @param customPlaceholders  Optional additional placeholder mappings
     */
    public void send(@Nullable CommandSender sender, @NotNull String key, @Nullable Map<String, String> customPlaceholders) {
        if (sender == null) {
            return;
        }

        String raw = getRawMessage(key);
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }

        String message = format(raw, sender, customPlaceholders);
        if (message.trim().isEmpty()) {
            return;
        }

        sender.sendMessage(message);
    }

    /**
     * Formats a raw message string by substituting prefix, sender placeholders,
     * custom placeholders, and color codes.
     *
     * @param raw                 Raw unformatted message
     * @param sender              Context command sender
     * @param customPlaceholders  Additional placeholder mappings
     * @return Fully formatted and colorized string
     */
    @NotNull
    public String format(@NotNull String raw, @Nullable CommandSender sender, @Nullable Map<String, String> customPlaceholders) {
        String message = raw.replace("{prefix}", prefix);
        message = PlaceholderUtil.applySender(message, sender);
        if (customPlaceholders != null && !customPlaceholders.isEmpty()) {
            message = PlaceholderUtil.applyMap(message, customPlaceholders);
        }
        if (sender instanceof org.bukkit.entity.Player player && isPlaceholderApiAvailable()) {
            try {
                message = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message);
            } catch (Throwable ignored) {
            }
        }
        return ColorUtil.colorize(message);
    }

    private boolean isPlaceholderApiAvailable() {
        try {
            return org.bukkit.Bukkit.getPluginManager() != null &&
                   org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Retrieves the raw string message associated with a key from cache or fallback.
     *
     * @param key Message key
     * @return Raw string, or null if key does not exist
     */
    @Nullable
    public String getRawMessage(@NotNull String key) {
        String msg = messagesCache.get(key);
        if (msg == null) {
            msg = defaultCache.get(key);
        }
        return msg;
    }

    @NotNull
    public String getPrefix() {
        return prefix;
    }
}
