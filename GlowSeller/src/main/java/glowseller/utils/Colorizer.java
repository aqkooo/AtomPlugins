package glowseller.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Colorizer {
    private static final Pattern HEX_PATTERN_1 = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    private Colorizer() {}

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Handle &#RRGGBB
        Matcher matcher1 = HEX_PATTERN_1.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher1.find()) {
            try {
                matcher1.appendReplacement(buffer, ChatColor.of("#" + matcher1.group(1)).toString());
            } catch (NoSuchMethodError | Exception e) {
                matcher1.appendReplacement(buffer, "");
            }
        }
        matcher1.appendTail(buffer);
        text = buffer.toString();

        // Handle <#RRGGBB>
        Matcher matcher2 = HEX_PATTERN_2.matcher(text);
        buffer = new StringBuffer();
        while (matcher2.find()) {
            try {
                matcher2.appendReplacement(buffer, ChatColor.of("#" + matcher2.group(1)).toString());
            } catch (NoSuchMethodError | Exception e) {
                matcher2.appendReplacement(buffer, "");
            }
        }
        matcher2.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static List<String> colorize(List<String> lines) {
        if (lines == null) return new ArrayList<>();
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(colorize(line));
        }
        return result;
    }

    public static String stripColor(String text) {
        if (text == null) return null;
        return ChatColor.stripColor(colorize(text));
    }
}
