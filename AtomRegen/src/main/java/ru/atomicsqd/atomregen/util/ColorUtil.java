package ru.atomicsqd.atomregen.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * High-performance color utility supporting:
 * - MiniMessage format (<gradient:#FF8A00:#E52E71>...</gradient>, <red>, etc.)
 * - HEX formatting (&#RRGGBB and <#RRGGBB>)
 * - Standard legacy codes (&a, &e, &l, &r)
 */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern ALT_HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ColorUtil() {}

    /**
     * Colorizes a string into legacy formatted string with full HEX & gradient support.
     */
    public static String color(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 1. Process MiniMessage tags if present (e.g. <gradient:...>)
        if (text.contains("<") && text.contains(">")) {
            try {
                Component comp = MINI_MESSAGE.deserialize(text);
                text = LEGACY_SERIALIZER.serialize(comp);
            } catch (Exception ignored) {
                // Fallback to manual hex replacement if MiniMessage fails
            }
        }

        // 2. Process &#RRGGBB format
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(sb, "§x"
                    + "§" + hex.charAt(0) + "§" + hex.charAt(1)
                    + "§" + hex.charAt(2) + "§" + hex.charAt(3)
                    + "§" + hex.charAt(4) + "§" + hex.charAt(5));
        }
        matcher.appendTail(sb);
        text = sb.toString();

        // 3. Process <#RRGGBB> format
        Matcher altMatcher = ALT_HEX_PATTERN.matcher(text);
        StringBuilder altSb = new StringBuilder();
        while (altMatcher.find()) {
            String hex = altMatcher.group(1);
            altMatcher.appendReplacement(altSb, "§x"
                    + "§" + hex.charAt(0) + "§" + hex.charAt(1)
                    + "§" + hex.charAt(2) + "§" + hex.charAt(3)
                    + "§" + hex.charAt(4) + "§" + hex.charAt(5));
        }
        altMatcher.appendTail(altSb);
        text = altSb.toString();

        // 4. Translate classic '&' color codes
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Colorizes a list of strings (e.g. item lore).
     */
    public static List<String> color(List<String> list) {
        if (list == null) return List.of();
        return list.stream().map(ColorUtil::color).collect(Collectors.toList());
    }

    /**
     * Deserializes a string into an Adventure Component.
     */
    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        if (text.contains("<") && text.contains(">")) {
            try {
                return MINI_MESSAGE.deserialize(text);
            } catch (Exception ignored) {
                // Fallback
            }
        }
        return LEGACY_SERIALIZER.deserialize(color(text));
    }
}
