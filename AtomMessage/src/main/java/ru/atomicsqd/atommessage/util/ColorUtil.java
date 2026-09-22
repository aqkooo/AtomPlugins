package ru.atomicsqd.atommessage.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern SECTION_HEX_PATTERN = Pattern.compile("§x§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])");

    private ColorUtil() {
    }

    /**
     * Converts mixed text (MiniMessage, hex, legacy & / § codes) into an Adventure Component.
     */
    @NotNull
    public static Component parseComponent(@Nullable String text, @Nullable Player player) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // 1. Process PlaceholderAPI if installed and player provided
        String processed = applyPlaceholders(text, player);

        // 2. Preprocess legacy codes and hex codes into MiniMessage format
        String mmText = convertLegacyToMiniMessage(processed);

        // 3. Deserialize via MiniMessage
        try {
            return MINI_MESSAGE.deserialize(mmText);
        } catch (Exception ex) {
            // Fallback: translate legacy directly
            String legacy = translateHexToSection(processed).replace('&', '§');
            return LEGACY_SERIALIZER.deserialize(legacy);
        }
    }

    /**
     * Converts mixed text into a legacy formatted string (with § codes and §x§... hex).
     */
    @NotNull
    public static String toLegacyString(@Nullable String text, @Nullable Player player) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Component component = parseComponent(text, player);
        return LEGACY_SERIALIZER.serialize(component);
    }

    /**
     * Strips all formatting and returns clean plain text.
     */
    @NotNull
    public static String stripColor(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Component component = parseComponent(text, null);
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Translates &#RRGGBB into §x§r§r§g§g§b§b format.
     */
    @NotNull
    public static String translateHexToSection(@NotNull String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(Character.toLowerCase(c));
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Converts & codes, § codes, and hex into MiniMessage compatible tags.
     */
    @NotNull
    public static String convertLegacyToMiniMessage(@NotNull String text) {
        // Handle &#RRGGBB -> <#RRGGBB>
        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        text = hexMatcher.replaceAll("<#$1>");

        // Handle §x§r§r§g§g§b§b -> <#RRGGBB>
        Matcher sectionHexMatcher = SECTION_HEX_PATTERN.matcher(text);
        text = sectionHexMatcher.replaceAll("<#$1$2$3$4$5$6>");

        // Replace legacy color codes with MiniMessage tags
        StringBuilder sb = new StringBuilder(text.length());
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < length) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                String tag = getMiniMessageTagForLegacy(code);
                if (tag != null) {
                    sb.append(tag);
                    i++; // skip code char
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @Nullable
    private static String getMiniMessageTagForLegacy(char code) {
        return switch (code) {
            case '0' -> "<reset><black>";
            case '1' -> "<reset><dark_blue>";
            case '2' -> "<reset><dark_green>";
            case '3' -> "<reset><dark_aqua>";
            case '4' -> "<reset><dark_red>";
            case '5' -> "<reset><dark_purple>";
            case '6' -> "<reset><gold>";
            case '7' -> "<reset><gray>";
            case '8' -> "<reset><dark_gray>";
            case '9' -> "<reset><blue>";
            case 'a' -> "<reset><green>";
            case 'b' -> "<reset><aqua>";
            case 'c' -> "<reset><red>";
            case 'd' -> "<reset><light_purple>";
            case 'e' -> "<reset><yellow>";
            case 'f' -> "<reset><white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    @NotNull
    private static String applyPlaceholders(@NotNull String text, @Nullable Player player) {
        if (player != null && Bukkit.getServer() != null) {
            try {
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    return PlaceholderAPI.setPlaceholders(player, text);
                }
            } catch (Throwable ignored) {
            }
        }
        return text;
    }
}
