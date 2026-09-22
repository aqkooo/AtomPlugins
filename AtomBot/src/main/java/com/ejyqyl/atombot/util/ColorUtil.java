package com.ejyqyl.atombot.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Color and styling utility for AtomBot.
 * Supports MiniMessage gradient tags, hex codes, legacy Bukkit codes,
 * and automatic stripping of raw closing tags.
 *
 * @author ejyqyl
 */
public final class ColorUtil {

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("(?i)<gradient:(#[A-Fa-f0-9]{6}):(#[A-Fa-f0-9]{6})>(.*?)</gradient>");
    private static final Pattern RAINBOW_PATTERN = Pattern.compile("(?i)<rainbow>(.*?)</rainbow>");
    private static final Pattern HEX_TAG_PATTERN = Pattern.compile("(?i)<#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_CLOSING_PATTERN = Pattern.compile("(?i)</#[A-Fa-f0-9]{6}>|</color>");
    private static final Pattern AMP_HEX_PATTERN = Pattern.compile("(?i)&#([A-Fa-f0-9]{6})");

    private static final Map<String, String> TAG_REPLACEMENTS = new HashMap<>();

    static {
        TAG_REPLACEMENTS.put("<bold>", "§l");
        TAG_REPLACEMENTS.put("<b>", "§l");
        TAG_REPLACEMENTS.put("</bold>", "§r");
        TAG_REPLACEMENTS.put("</b>", "§r");
        TAG_REPLACEMENTS.put("<italic>", "§o");
        TAG_REPLACEMENTS.put("<i>", "§o");
        TAG_REPLACEMENTS.put("</italic>", "§r");
        TAG_REPLACEMENTS.put("</i>", "§r");
        TAG_REPLACEMENTS.put("<underlined>", "§n");
        TAG_REPLACEMENTS.put("<u>", "§n");
        TAG_REPLACEMENTS.put("</underlined>", "§r");
        TAG_REPLACEMENTS.put("</u>", "§r");
        TAG_REPLACEMENTS.put("<strikethrough>", "§m");
        TAG_REPLACEMENTS.put("<s>", "§m");
        TAG_REPLACEMENTS.put("</strikethrough>", "§r");
        TAG_REPLACEMENTS.put("</s>", "§r");
        TAG_REPLACEMENTS.put("<obfuscated>", "§k");
        TAG_REPLACEMENTS.put("<obf>", "§k");
        TAG_REPLACEMENTS.put("</obfuscated>", "§r");
        TAG_REPLACEMENTS.put("</obf>", "§r");
        TAG_REPLACEMENTS.put("<reset>", "§r");
        TAG_REPLACEMENTS.put("<r>", "§r");

        TAG_REPLACEMENTS.put("<black>", "§0");
        TAG_REPLACEMENTS.put("<dark_blue>", "§1");
        TAG_REPLACEMENTS.put("<dark_green>", "§2");
        TAG_REPLACEMENTS.put("<dark_aqua>", "§3");
        TAG_REPLACEMENTS.put("<dark_red>", "§4");
        TAG_REPLACEMENTS.put("<dark_purple>", "§5");
        TAG_REPLACEMENTS.put("<gold>", "§6");
        TAG_REPLACEMENTS.put("<gray>", "§7");
        TAG_REPLACEMENTS.put("<dark_gray>", "§8");
        TAG_REPLACEMENTS.put("<blue>", "§9");
        TAG_REPLACEMENTS.put("<green>", "§a");
        TAG_REPLACEMENTS.put("<aqua>", "§b");
        TAG_REPLACEMENTS.put("<red>", "§c");
        TAG_REPLACEMENTS.put("<light_purple>", "§d");
        TAG_REPLACEMENTS.put("<yellow>", "§e");
        TAG_REPLACEMENTS.put("<white>", "§f");
    }

    private ColorUtil() {}

    @NotNull
    public static Component parse(@Nullable String text) {
        if (text == null) return Component.empty();
        String colorized = colorize(text);
        return LegacyComponentSerializer.legacySection().deserialize(colorized != null ? colorized : "");
    }

    @Nullable
    public static String colorize(@Nullable String text) {
        if (text == null) return null;
        if (text.isEmpty()) return text;

        text = applyGradients(text);
        text = applyRainbow(text);

        for (Map.Entry<String, String> entry : TAG_REPLACEMENTS.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }

        text = HEX_CLOSING_PATTERN.matcher(text).replaceAll("");

        Matcher hexTagMatcher = HEX_TAG_PATTERN.matcher(text);
        StringBuffer sbTag = new StringBuffer();
        while (hexTagMatcher.find()) {
            String hex = hexTagMatcher.group(1);
            hexTagMatcher.appendReplacement(sbTag, toChatColorHex(hex));
        }
        hexTagMatcher.appendTail(sbTag);
        text = sbTag.toString();

        Matcher ampHexMatcher = AMP_HEX_PATTERN.matcher(text);
        StringBuffer sbAmp = new StringBuffer();
        while (ampHexMatcher.find()) {
            String hex = ampHexMatcher.group(1);
            ampHexMatcher.appendReplacement(sbAmp, toChatColorHex(hex));
        }
        ampHexMatcher.appendTail(sbAmp);
        text = sbAmp.toString();

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @NotNull
    private static String applyGradients(@NotNull String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);
            String content = matcher.group(3);

            String gradientContent = createGradient(content, startHex, endHex);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(gradientContent));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @NotNull
    private static String applyRainbow(@NotNull String text) {
        Matcher matcher = RAINBOW_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String content = matcher.group(1);
            String rainbowContent = createRainbow(content);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(rainbowContent));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @NotNull
    private static String createGradient(@NotNull String text, @NotNull String startHex, @NotNull String endHex) {
        Color c1 = Color.decode(startHex);
        Color c2 = Color.decode(endHex);
        int length = text.length();
        if (length <= 1) {
            return toChatColorHex(startHex.replace("#", "")) + text;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) (length - 1);
            int r = (int) (c1.getRed() + ratio * (c2.getRed() - c1.getRed()));
            int g = (int) (c1.getGreen() + ratio * (c2.getGreen() - c1.getGreen()));
            int b = (int) (c1.getBlue() + ratio * (c2.getBlue() - c1.getBlue()));

            String hex = String.format("%02x%02x%02x", r, g, b);
            result.append(toChatColorHex(hex)).append(text.charAt(i));
        }
        return result.toString();
    }

    @NotNull
    private static String createRainbow(@NotNull String text) {
        int length = text.length();
        if (length == 0) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            float hue = (float) i / (float) length;
            Color color = Color.getHSBColor(hue, 0.8f, 1.0f);
            String hex = String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            result.append(toChatColorHex(hex)).append(text.charAt(i));
        }
        return result.toString();
    }

    @NotNull
    private static String toChatColorHex(@NotNull String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append('§').append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}
