package com.ejyqyl.glowchatgame.util;

import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal color utility for GlowChatGame.
 * Supports MiniMessage tags (gradients, rainbow, hex tags, named tags),
 * legacy &amp; color codes, and Spigot/Bungee hex syntax with embedded formatting style support.
 *
 * @author ejyqyl, Glowdevv
 */
public final class ColorUtil {

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("(?i)<gradient:(#[A-Fa-f0-9]{6}):(#[A-Fa-f0-9]{6})>(.*?)</gradient>");
    private static final Pattern RAINBOW_PATTERN = Pattern.compile("(?i)<rainbow>(.*?)</rainbow>");
    private static final Pattern HEX_TAG_PATTERN = Pattern.compile("(?i)<#([A-Fa-f0-9]{6})>");
    private static final Pattern AMP_HEX_PATTERN = Pattern.compile("(?i)&#([A-Fa-f0-9]{6})");

    private static final Map<String, String> TAG_REPLACEMENTS = new HashMap<>();

    static {
        // Formats
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

        // Named colors
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

    private ColorUtil() {
    }

    @NotNull
    public static String colorize(@NotNull String text) {
        if (text.isEmpty()) {
            return text;
        }

        // 1. Process MiniMessage gradients: <gradient:#HEX1:#HEX2>text</gradient>
        text = applyGradients(text);

        // 2. Process MiniMessage rainbow: <rainbow>text</rainbow>
        text = applyRainbow(text);

        // 3. Process named tags (<bold>, <yellow>, <reset>, etc.)
        for (Map.Entry<String, String> entry : TAG_REPLACEMENTS.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }

        // 4. Process <#RRGGBB>
        Matcher hexTagMatcher = HEX_TAG_PATTERN.matcher(text);
        StringBuffer sbTag = new StringBuffer();
        while (hexTagMatcher.find()) {
            String hex = hexTagMatcher.group(1);
            hexTagMatcher.appendReplacement(sbTag, toChatColorHex(hex));
        }
        hexTagMatcher.appendTail(sbTag);
        text = sbTag.toString();

        // 5. Process &#RRGGBB
        Matcher ampHexMatcher = AMP_HEX_PATTERN.matcher(text);
        StringBuffer sbAmp = new StringBuffer();
        while (ampHexMatcher.find()) {
            String hex = ampHexMatcher.group(1);
            ampHexMatcher.appendReplacement(sbAmp, toChatColorHex(hex));
        }
        ampHexMatcher.appendTail(sbAmp);
        text = sbAmp.toString();

        // 6. Process standard Bukkit legacy codes (&a, &l, &r...)
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
    public static String createGradient(@NotNull String text, @NotNull String fromHex, @NotNull String toHex) {
        if (text.isEmpty()) {
            return text;
        }

        StringBuilder cleanText = new StringBuilder();
        List<String> charStyles = new ArrayList<>();
        parseFormatting(text, cleanText, charStyles);

        int length = cleanText.length();
        if (length == 0) {
            return "";
        }

        Color from = Color.decode(fromHex);
        Color to = Color.decode(toHex);

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            float ratio = length == 1 ? 0f : (float) i / (float) (length - 1);
            int red = (int) (from.getRed() + ratio * (to.getRed() - from.getRed()));
            int green = (int) (from.getGreen() + ratio * (to.getGreen() - from.getGreen()));
            int blue = (int) (from.getBlue() + ratio * (to.getBlue() - from.getBlue()));

            String hex = String.format("%02x%02x%02x", red, green, blue);
            result.append(toChatColorHex(hex)).append(charStyles.get(i)).append(cleanText.charAt(i));
        }
        return result.toString();
    }

    @NotNull
    public static String createRainbow(@NotNull String text) {
        if (text.isEmpty()) {
            return text;
        }

        StringBuilder cleanText = new StringBuilder();
        List<String> charStyles = new ArrayList<>();
        parseFormatting(text, cleanText, charStyles);

        int length = cleanText.length();
        if (length == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            float hue = (float) i / (float) Math.max(1, length);
            Color color = Color.getHSBColor(hue, 0.85f, 1.0f);
            String hex = String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            result.append(toChatColorHex(hex)).append(charStyles.get(i)).append(cleanText.charAt(i));
        }
        return result.toString();
    }

    private static void parseFormatting(String text, StringBuilder cleanText, List<String> charStyles) {
        StringBuilder activeStyles = new StringBuilder();
        int i = 0;
        int len = text.length();

        while (i < len) {
            // Check for &l, &o, &n, &m, &k or §l, §o, §n, §m, §k
            if (i + 1 < len && (text.charAt(i) == '&' || text.charAt(i) == '§')) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (code == 'l' || code == 'o' || code == 'n' || code == 'm' || code == 'k') {
                    String s = "§" + code;
                    if (!activeStyles.toString().contains(s)) {
                        activeStyles.append(s);
                    }
                    i += 2;
                    continue;
                } else if (code == 'r') {
                    activeStyles.setLength(0);
                    i += 2;
                    continue;
                }
            }

            // Tags
            if (text.startsWith("<bold>", i)) { addStyle(activeStyles, "§l"); i += 6; continue; }
            if (text.startsWith("<b>", i)) { addStyle(activeStyles, "§l"); i += 3; continue; }
            if (text.startsWith("</bold>", i)) { removeStyle(activeStyles, "§l"); i += 7; continue; }
            if (text.startsWith("</b>", i)) { removeStyle(activeStyles, "§l"); i += 4; continue; }

            if (text.startsWith("<italic>", i)) { addStyle(activeStyles, "§o"); i += 8; continue; }
            if (text.startsWith("<i>", i)) { addStyle(activeStyles, "§o"); i += 3; continue; }
            if (text.startsWith("</italic>", i)) { removeStyle(activeStyles, "§o"); i += 9; continue; }
            if (text.startsWith("</i>", i)) { removeStyle(activeStyles, "§o"); i += 4; continue; }

            if (text.startsWith("<underlined>", i)) { addStyle(activeStyles, "§n"); i += 12; continue; }
            if (text.startsWith("<u>", i)) { addStyle(activeStyles, "§n"); i += 3; continue; }
            if (text.startsWith("</underlined>", i)) { removeStyle(activeStyles, "§n"); i += 13; continue; }
            if (text.startsWith("</u>", i)) { removeStyle(activeStyles, "§n"); i += 4; continue; }

            if (text.startsWith("<strikethrough>", i)) { addStyle(activeStyles, "§m"); i += 15; continue; }
            if (text.startsWith("<s>", i)) { addStyle(activeStyles, "§m"); i += 3; continue; }
            if (text.startsWith("</strikethrough>", i)) { removeStyle(activeStyles, "§m"); i += 16; continue; }
            if (text.startsWith("</s>", i)) { removeStyle(activeStyles, "§m"); i += 4; continue; }

            if (text.startsWith("<obfuscated>", i)) { addStyle(activeStyles, "§k"); i += 12; continue; }
            if (text.startsWith("<obf>", i)) { addStyle(activeStyles, "§k"); i += 5; continue; }
            if (text.startsWith("</obfuscated>", i)) { removeStyle(activeStyles, "§k"); i += 13; continue; }
            if (text.startsWith("</obf>", i)) { removeStyle(activeStyles, "§k"); i += 6; continue; }

            if (text.startsWith("<reset>", i)) { activeStyles.setLength(0); i += 7; continue; }
            if (text.startsWith("<r>", i)) { activeStyles.setLength(0); i += 3; continue; }

            cleanText.append(text.charAt(i));
            charStyles.add(activeStyles.toString());
            i++;
        }
    }

    private static void addStyle(StringBuilder sb, String style) {
        if (!sb.toString().contains(style)) {
            sb.append(style);
        }
    }

    private static void removeStyle(StringBuilder sb, String style) {
        int idx = sb.indexOf(style);
        if (idx != -1) {
            sb.delete(idx, idx + style.length());
        }
    }

    @NotNull
    private static String toChatColorHex(@NotNull String hex) {
        try {
            return ChatColor.of("#" + hex).toString();
        } catch (Throwable ignored) {
            StringBuilder sb = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                sb.append('§').append(Character.toLowerCase(c));
            }
            return sb.toString();
        }
    }
}
