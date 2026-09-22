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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal text processing, color formatting, and interactive components engine.
 * Supports:
 * - Legacy Minecraft colors (&0-&f, §0-§f) and formatting codes (&l, &m, &n, &o, &r, &k)
 * - HEX colors: &#RRGGBB, {#RRGGBB}, <#RRGGBB>, &x&r&r&g&g&b&b, §x§r§r§g§g§b§b
 * - Adventure MiniMessage: gradients, rainbows, transitions, shadows, full tag set
 * - Interactive BBCode tags:
 *     [url=https://... hover="..."]Text[/url] -> Clickable link with tooltip
 *     [cmd=/command hover="..."]Text[/cmd] -> Run command on click with tooltip
 *     [suggest=/command hover="..."]Text[/suggest] -> Suggest command in chat
 *     [copy=value hover="..."]Text[/copy] -> Copy to clipboard
 *     [hover="..."]Text[/hover] -> Hover tooltip
 * - Native MiniMessage interactive tags (<click:...>, <hover:...>)
 * - PlaceholderAPI placeholders: %placeholder%
 */
public final class ColorUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    // Regex patterns for all HEX color variants
    private static final Pattern SPIGOT_HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern BRACE_HEX_PATTERN = Pattern.compile("\\{#([A-Fa-f0-9]{6})\\}");
    private static final Pattern AMPERSAND_X_HEX_PATTERN = Pattern.compile("&x&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])");
    private static final Pattern SECTION_X_HEX_PATTERN = Pattern.compile("§x§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])§([A-Fa-f0-9])");

    // BBCode Interactive Tags Patterns (supporting spaces and quotes in attributes)
    private static final Pattern BB_URL_PATTERN = Pattern.compile("\\[url=(?:\"([^\"]+)\"|'([^']+)'|([^\\]]+?))(?:\\s+hover=(?:\"([^\"]*)\"|'([^']*)'|([^\\s\\]]+)))?\\](.*?)\\[/url\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern BB_CMD_PATTERN = Pattern.compile("\\[cmd=(?:\"([^\"]+)\"|'([^']+)'|([^\\]]+?))(?:\\s+hover=(?:\"([^\"]*)\"|'([^']*)'|([^\\s\\]]+)))?\\](.*?)\\[/cmd\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern BB_SUGGEST_PATTERN = Pattern.compile("\\[suggest=(?:\"([^\"]+)\"|'([^']+)'|([^\\]]+?))(?:\\s+hover=(?:\"([^\"]*)\"|'([^']*)'|([^\\s\\]]+)))?\\](.*?)\\[/suggest\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern BB_COPY_PATTERN = Pattern.compile("\\[copy=(?:\"([^\"]+)\"|'([^']+)'|([^\\]]+?))(?:\\s+hover=(?:\"([^\"]*)\"|'([^']*)'|([^\\s\\]]+)))?\\](.*?)\\[/copy\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern BB_HOVER_PATTERN = Pattern.compile("\\[hover=(?:\"([^\"]*)\"|'([^']*)'|([^\\s\\]]+))\\](.*?)\\[/hover\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private ColorUtil() {
    }

    /**
     * Converts mixed text into an Adventure Component.
     */
    @NotNull
    public static Component parseComponent(@Nullable String text, @Nullable Player player) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // 1. Process PlaceholderAPI if installed and player provided
        String processed = applyPlaceholders(text, player);

        // 2. Preprocess interactive BBCode tags, all HEX formats, and legacy color codes
        String mmText = preprocessAllFormatting(processed);

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
     * Parses a list of strings into a list of Components.
     */
    @NotNull
    public static List<Component> parseComponentList(@Nullable List<String> lines, @Nullable Player player) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<Component> list = new ArrayList<>(lines.size());
        for (String line : lines) {
            list.add(parseComponent(line, player));
        }
        return list;
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
     * Preprocesses all color formats, interactive tags, and legacy codes into MiniMessage compatible text.
     */
    @NotNull
    public static String preprocessAllFormatting(@NotNull String text) {
        // 1. Convert interactive BBCode tags
        text = convertBBCodeTags(text);

        // 2. Convert all HEX formats to <#RRGGBB>
        // Spigot: &#RRGGBB -> <#RRGGBB>
        text = SPIGOT_HEX_PATTERN.matcher(text).replaceAll("<#$1>");

        // CMI / Essentials: {#RRGGBB} -> <#RRGGBB>
        text = BRACE_HEX_PATTERN.matcher(text).replaceAll("<#$1>");

        // Ampersand X: &x&r&r&g&g&b&b -> <#RRGGBB>
        text = AMPERSAND_X_HEX_PATTERN.matcher(text).replaceAll("<#$1$2$3$4$5$6>");

        // Section X: §x§r§r§g§g§b§b -> <#RRGGBB>
        text = SECTION_X_HEX_PATTERN.matcher(text).replaceAll("<#$1$2$3$4$5$6>");

        // 3. Replace legacy & and § color codes with MiniMessage tags
        text = convertLegacyCodes(text);

        return text;
    }

    /**
     * Converts BBCode-style interactive tags into MiniMessage tags.
     */
    @NotNull
    public static String convertBBCodeTags(@NotNull String text) {
        // [url=URL hover="..."]Text[/url]
        Matcher urlMatcher = BB_URL_PATTERN.matcher(text);
        if (urlMatcher.find()) {
            StringBuffer sb = new StringBuffer();
            do {
                String url = getFirstNonNull(urlMatcher.group(1), urlMatcher.group(2), urlMatcher.group(3));
                String hover = getFirstNonNull(urlMatcher.group(4), urlMatcher.group(5), urlMatcher.group(6));
                String content = urlMatcher.group(7);
                String hoverTag = hover != null && !hover.isEmpty()
                        ? "<hover:show_text:'" + escapeSingleQuotes(hover) + "'>"
                        : "<hover:show_text:'<gray>Нажмите, чтобы открыть ссылку</gray>'>";
                String replacement = "<click:open_url:'" + escapeSingleQuotes(url != null ? url : "") + "'>" + hoverTag + content + "</hover></click>";
                urlMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } while (urlMatcher.find());
            urlMatcher.appendTail(sb);
            text = sb.toString();
        }

        // [cmd=/command hover="..."]Text[/cmd]
        Matcher cmdMatcher = BB_CMD_PATTERN.matcher(text);
        if (cmdMatcher.find()) {
            StringBuffer sb = new StringBuffer();
            do {
                String cmd = getFirstNonNull(cmdMatcher.group(1), cmdMatcher.group(2), cmdMatcher.group(3));
                String hover = getFirstNonNull(cmdMatcher.group(4), cmdMatcher.group(5), cmdMatcher.group(6));
                String content = cmdMatcher.group(7);
                String hoverTag = hover != null && !hover.isEmpty()
                        ? "<hover:show_text:'" + escapeSingleQuotes(hover) + "'>"
                        : "<hover:show_text:'<gray>Нажмите, чтобы выполнить: <yellow>" + escapeSingleQuotes(cmd != null ? cmd : "") + "</yellow></gray>'>";
                String replacement = "<click:run_command:'" + escapeSingleQuotes(cmd != null ? cmd : "") + "'>" + hoverTag + content + "</hover></click>";
                cmdMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } while (cmdMatcher.find());
            cmdMatcher.appendTail(sb);
            text = sb.toString();
        }

        // [suggest=/command hover="..."]Text[/suggest]
        Matcher suggestMatcher = BB_SUGGEST_PATTERN.matcher(text);
        if (suggestMatcher.find()) {
            StringBuffer sb = new StringBuffer();
            do {
                String cmd = getFirstNonNull(suggestMatcher.group(1), suggestMatcher.group(2), suggestMatcher.group(3));
                String hover = getFirstNonNull(suggestMatcher.group(4), suggestMatcher.group(5), suggestMatcher.group(6));
                String content = suggestMatcher.group(7);
                String hoverTag = hover != null && !hover.isEmpty()
                        ? "<hover:show_text:'" + escapeSingleQuotes(hover) + "'>"
                        : "<hover:show_text:'<gray>Нажмите, чтобы вставить команду</gray>'>";
                String replacement = "<click:suggest_command:'" + escapeSingleQuotes(cmd != null ? cmd : "") + "'>" + hoverTag + content + "</hover></click>";
                suggestMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } while (suggestMatcher.find());
            suggestMatcher.appendTail(sb);
            text = sb.toString();
        }

        // [copy=VALUE hover="..."]Text[/copy]
        Matcher copyMatcher = BB_COPY_PATTERN.matcher(text);
        if (copyMatcher.find()) {
            StringBuffer sb = new StringBuffer();
            do {
                String val = getFirstNonNull(copyMatcher.group(1), copyMatcher.group(2), copyMatcher.group(3));
                String hover = getFirstNonNull(copyMatcher.group(4), copyMatcher.group(5), copyMatcher.group(6));
                String content = copyMatcher.group(7);
                String hoverTag = hover != null && !hover.isEmpty()
                        ? "<hover:show_text:'" + escapeSingleQuotes(hover) + "'>"
                        : "<hover:show_text:'<gray>Нажмите, чтобы скопировать: <yellow>" + escapeSingleQuotes(val != null ? val : "") + "</yellow></gray>'>";
                String replacement = "<click:copy_to_clipboard:'" + escapeSingleQuotes(val != null ? val : "") + "'>" + hoverTag + content + "</hover></click>";
                copyMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } while (copyMatcher.find());
            copyMatcher.appendTail(sb);
            text = sb.toString();
        }

        // [hover="..."]Text[/hover]
        Matcher hoverMatcher = BB_HOVER_PATTERN.matcher(text);
        if (hoverMatcher.find()) {
            StringBuffer sb = new StringBuffer();
            do {
                String hover = getFirstNonNull(hoverMatcher.group(1), hoverMatcher.group(2), hoverMatcher.group(3));
                String content = hoverMatcher.group(4);
                String replacement = "<hover:show_text:'" + escapeSingleQuotes(hover != null ? hover : "") + "'>" + content + "</hover>";
                hoverMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } while (hoverMatcher.find());
            hoverMatcher.appendTail(sb);
            text = sb.toString();
        }

        return text;
    }

    /**
     * Translates &#RRGGBB and {#RRGGBB} into §x§r§r§g§g§b§b format.
     */
    @NotNull
    public static String translateHexToSection(@NotNull String text) {
        Matcher matcher = SPIGOT_HEX_PATTERN.matcher(text);
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

        Matcher braceMatcher = BRACE_HEX_PATTERN.matcher(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        while (braceMatcher.find()) {
            String hex = braceMatcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(Character.toLowerCase(c));
            }
            braceMatcher.appendReplacement(sb2, Matcher.quoteReplacement(replacement.toString()));
        }
        braceMatcher.appendTail(sb2);

        return sb2.toString();
    }

    /**
     * Converts & codes and § codes into MiniMessage tags.
     */
    @NotNull
    public static String convertLegacyCodes(@NotNull String text) {
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
    private static String escapeSingleQuotes(@NotNull String str) {
        return str.replace("'", "\\'");
    }

    @Nullable
    private static String getFirstNonNull(String... items) {
        for (String item : items) {
            if (item != null) {
                return item;
            }
        }
        return null;
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
