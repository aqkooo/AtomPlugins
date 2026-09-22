package com.ejyqyl.glowchatgame.config;

import com.ejyqyl.glowchatgame.GlowChatGame;
import com.ejyqyl.glowchatgame.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Localized message manager with MiniMessage gradient, list-message, and PAPI support.
 *
 * @author ejyqyl, Glowdevv
 */
public final class MessageManager {

    private final GlowChatGame plugin;
    private final File messagesFile;
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private String prefix = "<gradient:#00B5FD:#7670E5>ɢʟᴏᴡᴄʜᴀᴛɢᴀᴍᴇ ≫</gradient>";

    public MessageManager(@NotNull GlowChatGame plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        reload();
    }

    public synchronized void reload() {
        cache.clear();

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(messagesFile);
        YamlConfiguration defaultConfig = null;

        try (InputStream stream = plugin.getResource("messages.yml")) {
            if (stream != null) {
                defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read embedded messages.yml: " + e.getMessage());
        }

        // Auto-repair missing keys non-destructively
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
                    plugin.getLogger().info("Repaired messages.yml: added missing default keys.");
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to save repaired messages.yml: " + e.getMessage());
                }
            }
        }

        // Cache all keys
        for (String key : userConfig.getKeys(true)) {
            if (!userConfig.isConfigurationSection(key)) {
                cache.put(key, userConfig.get(key));
            }
        }

        Object prefixObj = cache.get("prefix");
        if (prefixObj instanceof String p) {
            this.prefix = p;
        }
    }

    public void send(@Nullable CommandSender sender, @NotNull String key) {
        send(sender, key, Collections.emptyMap());
    }

    public void send(@Nullable CommandSender sender, @NotNull String key, @Nullable Map<String, String> placeholders) {
        if (sender == null) {
            return;
        }

        Object obj = cache.get(key);
        if (obj == null) {
            return;
        }

        if (obj instanceof List<?> list) {
            for (Object line : list) {
                String formatted = format(String.valueOf(line), sender, placeholders);
                sender.sendMessage(formatted);
            }
        } else {
            String raw = String.valueOf(obj);
            if (raw.trim().isEmpty()) {
                return;
            }
            String formatted = format(raw, sender, placeholders);
            sender.sendMessage(formatted);
        }
    }

    public void broadcast(@NotNull String key, @Nullable Map<String, String> placeholders) {
        Object obj = cache.get(key);
        if (obj == null) {
            return;
        }

        if (obj instanceof List<?> list) {
            for (Object line : list) {
                String formatted = format(String.valueOf(line), null, placeholders);
                Bukkit.broadcastMessage(formatted);
            }
        } else {
            String raw = String.valueOf(obj);
            if (raw.trim().isEmpty()) {
                return;
            }
            String formatted = format(raw, null, placeholders);
            Bukkit.broadcastMessage(formatted);
        }
    }

    @NotNull
    public String format(@NotNull String raw, @Nullable CommandSender sender, @Nullable Map<String, String> placeholders) {
        String message = raw.replace("{prefix}", prefix);

        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }

        if (sender instanceof Player player && isPlaceholderApiAvailable()) {
            try {
                message = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message);
            } catch (Throwable ignored) {
            }
        }

        return ColorUtil.colorize(message);
    }

    private boolean isPlaceholderApiAvailable() {
        try {
            return Bukkit.getPluginManager() != null &&
                   Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        } catch (Throwable t) {
            return false;
        }
    }

    @NotNull
    public String getPrefix() {
        return prefix;
    }
}
