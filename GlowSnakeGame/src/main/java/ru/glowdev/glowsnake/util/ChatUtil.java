package ru.glowdev.glowsnake.util;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ChatUtil() {}

    public static String color(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // Support hex colors: &#RRGGBB
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hexCode).toString());
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static List<String> color(List<String> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        List<String> colored = new ArrayList<>(list.size());
        for (String line : list) {
            colored.add(color(line));
        }
        return colored;
    }

    public static String formatItemText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String colored = color(text);
        if (!colored.startsWith("§r")) {
            return "§r" + colored;
        }
        return colored;
    }

    public static List<String> formatItemLore(List<String> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        List<String> formatted = new ArrayList<>(list.size());
        for (String line : list) {
            formatted.add(formatItemText(line));
        }
        return formatted;
    }

    public static String stripColor(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.stripColor(color(message));
    }
}
