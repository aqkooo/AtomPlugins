package me.ejyqyl.tituls.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {
    private static final Pattern HEX_HASH_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private TextUtil() {
    }

    /**
     * Translates &#RRGGBB into §x§r§r§g§g§b§b format.
     */
    @NotNull
    public static String translateHex(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

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

    /**
     * Converts a string with legacy (&, §, &#hex) or MiniMessage tags into an Adventure Component.
     */
    @NotNull
    public static Component toComponent(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // If it starts with or contains MiniMessage tags (<...>), we can try MiniMessage first
        if (text.contains("<") && text.contains(">")) {
            try {
                return MINI_MESSAGE.deserialize(text);
            } catch (Exception ignored) {
                // Fall back to legacy parsing
            }
        }

        String converted = translateHex(text);
        converted = converted.replace('&', '§');
        return SECTION_SERIALIZER.deserialize(converted);
    }

    /**
     * Converts to legacy string formatted with § codes (e.g. for PAPI).
     */
    @NotNull
    public static String toLegacy(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String converted = translateHex(text);
        return converted.replace('&', '§');
    }

    /**
     * Strips all formatting, returning pure plain text.
     */
    @NotNull
    public static String stripFormatting(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Component comp = toComponent(text);
        return PlainTextComponentSerializer.plainText().serialize(comp);
    }

    /**
     * Builds an ItemStack with non-italic display name and lore components.
     */
    @NotNull
    public static ItemStack createItem(@NotNull Material material, @Nullable String displayName, @Nullable List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                Component nameComp = toComponent(displayName).decoration(TextDecoration.ITALIC, false);
                meta.displayName(nameComp);
            }
            if (loreLines != null && !loreLines.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreLines) {
                    lore.add(toComponent(line).decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
