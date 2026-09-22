package com.ejyqyl.glowcmd.util;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * High-performance placeholder resolver for GlowCMD messages.
 *
 * @author ejyqyl
 */
public final class PlaceholderUtil {

    private PlaceholderUtil() {
    }

    /**
     * Applies standard sender-related placeholders ({PLAYER}, {USERNAME}, {DISPLAYNAME},
     * {WORLD}, {X}, {Y}, {Z}).
     *
     * @param text   Source text
     * @param sender Command sender (Player or Console)
     * @return Formatted string with substituted placeholders
     */
    @NotNull
    public static String applySender(@Nullable String text, @Nullable CommandSender sender) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (sender == null) {
            return text;
        }

        String name = sender.getName();
        String result = text.replace("{PLAYER}", name)
                            .replace("{USERNAME}", name);

        if (sender instanceof Player) {
            Player player = (Player) sender;
            result = result.replace("{DISPLAYNAME}", player.getDisplayName());
            Location loc = player.getLocation();
            if (loc.getWorld() != null) {
                result = result.replace("{WORLD}", loc.getWorld().getName());
            } else {
                result = result.replace("{WORLD}", "");
            }
            result = result.replace("{X}", String.valueOf(loc.getBlockX()))
                           .replace("{Y}", String.valueOf(loc.getBlockY()))
                           .replace("{Z}", String.valueOf(loc.getBlockZ()));
        } else {
            result = result.replace("{DISPLAYNAME}", name)
                           .replace("{WORLD}", "console")
                           .replace("{X}", "0")
                           .replace("{Y}", "0")
                           .replace("{Z}", "0");
        }

        return result;
    }

    /**
     * Applies location-specific placeholders ({WORLD}, {X}, {Y}, {Z}).
     *
     * @param text     Source text
     * @param location Target location
     * @return Formatted string
     */
    @NotNull
    public static String applyLocation(@Nullable String text, @Nullable Location location) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (location == null) {
            return text;
        }

        String result = text;
        if (location.getWorld() != null) {
            result = result.replace("{WORLD}", location.getWorld().getName());
        } else {
            result = result.replace("{WORLD}", "");
        }
        return result.replace("{X}", String.valueOf(location.getBlockX()))
                     .replace("{Y}", String.valueOf(location.getBlockY()))
                     .replace("{Z}", String.valueOf(location.getBlockZ()));
    }

    /**
     * Applies an arbitrary key-value mapping of placeholders.
     *
     * @param text         Source text
     * @param placeholders Key-value map of placeholders
     * @return Formatted string
     */
    @NotNull
    public static String applyMap(@Nullable String text, @Nullable Map<String, String> placeholders) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && value != null) {
                result = result.replace(key, value);
            }
        }
        return result;
    }
}
