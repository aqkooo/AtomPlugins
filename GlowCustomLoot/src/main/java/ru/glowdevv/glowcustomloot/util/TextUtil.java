package ru.glowdevv.glowcustomloot.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final Pattern HEX_HASH_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern SECTION_HEX_PATTERN = Pattern.compile("§x§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])");

    private TextUtil() {
    }

    /**
     * Parses text into an Adventure Component with italics explicitly disabled by default
     * to prevent default Minecraft anvil / renamed italic styling.
     */
    @NotNull
    public static Component parse(@Nullable String text) {
        return parse(text, true);
    }

    /**
     * Parses text into an Adventure Component with optional no-italic enforcement.
     */
    @NotNull
    public static Component parse(@Nullable String text, boolean forceNoItalic) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        String mmText = convertToMiniMessage(text);
        Component component;
        try {
            component = MINI_MESSAGE.deserialize(mmText);
        } catch (Exception ex) {
            String legacy = translateHexToSection(text).replace('&', '§');
            component = LEGACY_SERIALIZER.deserialize(legacy);
        }

        if (forceNoItalic) {
            // Explicitly disable italic unless the text specified <italic>
            if (!component.hasDecoration(TextDecoration.ITALIC)) {
                component = component.decoration(TextDecoration.ITALIC, false);
            }
        }
        return component;
    }

    /**
     * Parses a list of lines into lore components with no italics.
     */
    @NotNull
    public static List<Component> parseLore(@Nullable List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return new ArrayList<>();
        }
        List<Component> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(parse(line, true));
        }
        return result;
    }

    /**
     * Converts legacy & and § and hex codes into MiniMessage format.
     */
    @NotNull
    public static String convertToMiniMessage(@NotNull String text) {
        // Handle &#RRGGBB -> <#RRGGBB>
        Matcher hexMatcher = HEX_HASH_PATTERN.matcher(text);
        text = hexMatcher.replaceAll("<#$1>");

        // Handle §x§r§r§g§g§b§b -> <#RRGGBB>
        Matcher sectionHexMatcher = SECTION_HEX_PATTERN.matcher(text);
        text = sectionHexMatcher.replaceAll("<#$1$2$3$4$5$6>");

        StringBuilder sb = new StringBuilder(text.length());
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < length) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                String tag = getMiniMessageTag(code);
                if (tag != null) {
                    sb.append(tag);
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @Nullable
    private static String getMiniMessageTag(char code) {
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
    public static String translateHexToSection(@NotNull String text) {
        Matcher matcher = HEX_HASH_PATTERN.matcher(text);
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

    @NotNull
    public static String stripFormatting(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Component component = parse(text, false);
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @NotNull
    public static String toPlain(@Nullable Component component) {
        if (component == null) {
            return "";
        }
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
