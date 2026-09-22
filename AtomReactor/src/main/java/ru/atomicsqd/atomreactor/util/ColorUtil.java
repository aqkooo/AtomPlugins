package ru.atomicsqd.atomreactor.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Color and Typography utility strictly adhering to:
 * - NO ITALICS EVER (recursively disabled on all components)
 * - FULL GRADIENT SUPPORT (MiniMessage and HEX)
 */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern ALT_HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ColorUtil() {}

    /**
     * Converts formatted string (MiniMessage gradients or HEX) into an Adventure Component with ITALICS DISABLED.
     */
    public static Component component(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty().decoration(TextDecoration.ITALIC, false);
        }

        Component comp;
        if (text.contains("<") && text.contains(">")) {
            try {
                // Ensure MiniMessage deserializes with no italic tag if needed
                comp = MINI_MESSAGE.deserialize(text);
            } catch (Exception e) {
                comp = LEGACY_SERIALIZER.deserialize(colorize(text));
            }
        } else {
            comp = LEGACY_SERIALIZER.deserialize(colorize(text));
        }

        return cleanItalics(comp);
    }

    /**
     * Converts a list of strings into components with italics disabled.
     */
    public static List<Component> componentList(List<String> list) {
        if (list == null) return new ArrayList<>();
        List<Component> result = new ArrayList<>(list.size());
        for (String s : list) {
            result.add(component(s));
        }
        return result;
    }

    /**
     * Recursively strips any italic decoration from the component and all its children.
     */
    public static Component cleanItalics(Component component) {
        if (component == null) {
            return Component.empty().decoration(TextDecoration.ITALIC, false);
        }
        return component.decoration(TextDecoration.ITALIC, false)
                .children(component.children().stream().map(ColorUtil::cleanItalics).toList());
    }

    /**
     * Translates HEX and MiniMessage into a legacy string (for item meta where needed).
     */
    @SuppressWarnings("deprecation")
    public static String colorize(String text) {
        if (text == null || text.isEmpty()) return "";

        if (text.contains("<") && text.contains(">")) {
            try {
                Component comp = MINI_MESSAGE.deserialize(text);
                text = LEGACY_SERIALIZER.serialize(comp);
            } catch (Exception ignored) {}
        }

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder(text.length() + 32);
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder rep = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                rep.append('§').append(c);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(rep.toString()));
        }
        matcher.appendTail(buffer);
        text = buffer.toString();

        Matcher altMatcher = ALT_HEX_PATTERN.matcher(text);
        StringBuilder altBuffer = new StringBuilder(text.length() + 32);
        while (altMatcher.find()) {
            String hex = altMatcher.group(1);
            StringBuilder rep = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                rep.append('§').append(c);
            }
            altMatcher.appendReplacement(altBuffer, Matcher.quoteReplacement(rep.toString()));
        }
        altMatcher.appendTail(altBuffer);

        return ChatColor.translateAlternateColorCodes('&', altBuffer.toString());
    }
}
