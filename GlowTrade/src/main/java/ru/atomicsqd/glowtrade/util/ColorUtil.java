package ru.atomicsqd.glowtrade.util;

import net.md_5.bungee.api.ChatColor;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилита для работы с цветами и градиентами.
 * Поддерживает:
 *  - Фирменный градиент GlowTrade (#FFA500 -> #FFD700)
 *  - Парные теги градиента <gradient:#HEX1:#HEX2>текст</gradient>
 *  - Вложенные и незакрытые теги градиентов с гарантированной очисткой
 *  - Очистку от закрывающих тегов </#HEX>, </color>, </gradient>
 *  - Одиночные HEX-цвета <#HEX> и &#HEX
 *  - Стандартные цветовые коды (&a-&f, &0-&9, &l, &r и т.д.)
 *  - Очистку от навязчивого наковаленного курсива (§o / ITALIC) для элементов GUI
 */
public final class ColorUtil {

    // Шаблон для поиска самого глубокого (внутреннего) тега градиента без вложений
    private static final Pattern INNER_GRADIENT_PATTERN = Pattern.compile(
            "<gradient:(#[0-9a-fA-F]{6}):(#[0-9a-fA-F]{6})>((?:(?!<gradient:).)*?)</gradient>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Шаблон для незакрытого градиента (до конца строки или следующего открывающего тега)
    private static final Pattern UNCLOSED_GRADIENT_PATTERN = Pattern.compile(
            "<gradient:(#[0-9a-fA-F]{6}):(#[0-9a-fA-F]{6})>([^<]*)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern HEX_PATTERN_BRACKETS = Pattern.compile("<(#[0-9a-fA-F]{6})>");
    private static final Pattern HEX_PATTERN_AMPERSAND = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern CLOSING_TAGS_PATTERN = Pattern.compile("</(#[0-9a-fA-F]{6}|color|gradient)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANY_GRADIENT_TAG_REMAINDER = Pattern.compile("(?i)</?gradient(:[^>]*)?>");
    private static final Pattern RESIDUAL_CLOSING_PATTERN = Pattern.compile("</#[0-9a-fA-F]{6}>", Pattern.CASE_INSENSITIVE);

    public static final String DEFAULT_GRADIENT_START = "#FFA500";
    public static final String DEFAULT_GRADIENT_END = "#FFD700";

    public static final String RED_GRADIENT_START = "#FF5252";
    public static final String RED_GRADIENT_END = "#FF1744";

    public static final String GREEN_GRADIENT_START = "#00E676";
    public static final String GREEN_GRADIENT_END = "#1DE9B6";

    private ColorUtil() {}

    /**
     * Преобразует строку с любыми цветовыми тегами в отформатированную строку Bukkit.
     */
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // 1. Итеративная обработка парных градиентов (начиная с самых внутренних)
        int iterations = 0;
        while (iterations++ < 10) {
            Matcher gradientMatcher = INNER_GRADIENT_PATTERN.matcher(message);
            if (!gradientMatcher.find()) {
                break;
            }
            StringBuilder sb = new StringBuilder();
            do {
                String hexStart = gradientMatcher.group(1);
                String hexEnd = gradientMatcher.group(2);
                String content = gradientMatcher.group(3);
                gradientMatcher.appendReplacement(sb, Matcher.quoteReplacement(applyGradient(content, hexStart, hexEnd)));
            } while (gradientMatcher.find());
            gradientMatcher.appendTail(sb);
            message = sb.toString();
        }

        // 2. Обработка незакрытых градиентов (если кто-то случайно опустил </gradient>)
        Matcher unclosedMatcher = UNCLOSED_GRADIENT_PATTERN.matcher(message);
        if (unclosedMatcher.find()) {
            StringBuilder sb = new StringBuilder();
            do {
                String hexStart = unclosedMatcher.group(1);
                String hexEnd = unclosedMatcher.group(2);
                String content = unclosedMatcher.group(3);
                unclosedMatcher.appendReplacement(sb, Matcher.quoteReplacement(applyGradient(content, hexStart, hexEnd)));
            } while (unclosedMatcher.find());
            unclosedMatcher.appendTail(sb);
            message = sb.toString();
        }

        // 3. Гарантированная зачистка любых остаточных тегов <gradient:...> или </gradient>
        message = ANY_GRADIENT_TAG_REMAINDER.matcher(message).replaceAll("");

        // 4. Обработка одиночных HEX-тегов формата <#RRGGBB>
        Matcher hexBracketMatcher = HEX_PATTERN_BRACKETS.matcher(message);
        StringBuilder hexBracketSb = new StringBuilder();
        while (hexBracketMatcher.find()) {
            String hex = hexBracketMatcher.group(1);
            hexBracketMatcher.appendReplacement(hexBracketSb, ChatColor.of(hex).toString());
        }
        hexBracketMatcher.appendTail(hexBracketSb);
        message = hexBracketSb.toString();

        // 5. Обработка одиночных HEX-тегов формата &#RRGGBB
        Matcher hexAmpMatcher = HEX_PATTERN_AMPERSAND.matcher(message);
        StringBuilder hexAmpSb = new StringBuilder();
        while (hexAmpMatcher.find()) {
            String hex = "#" + hexAmpMatcher.group(1);
            hexAmpMatcher.appendReplacement(hexAmpSb, ChatColor.of(hex).toString());
        }
        hexAmpMatcher.appendTail(hexAmpSb);
        message = hexAmpSb.toString();

        // 6. Очистка любых оставшихся закрывающих тегов </#RRGGBB>, </color>, </gradient>
        message = CLOSING_TAGS_PATTERN.matcher(message).replaceAll("");
        message = RESIDUAL_CLOSING_PATTERN.matcher(message).replaceAll("");

        // 7. Обработка классических цветовых кодов (&a, &b, ...)
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Создает строковый градиент между двумя HEX-цветами для заданного текста.
     * Корректно сохраняет и переносит модификаторы форматирования (&l, &o, &n, &m).
     */
    public static String applyGradient(String text, String hexStart, String hexEnd) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Color start = Color.decode(hexStart);
        Color end = Color.decode(hexEnd);

        // Извлекаем только видимые символы для расчета шага градиента
        int visibleLength = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&' && i + 1 < text.length()) {
                i++; // Пропускаем цветовой код
                continue;
            }
            if (text.charAt(i) == '§' && i + 1 < text.length()) {
                i++; // Пропускаем уже скомпилированный цветовой код
                continue;
            }
            visibleLength++;
        }

        if (visibleLength == 0) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        StringBuilder currentFormats = new StringBuilder();
        int stepIndex = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Обработка кодов форматирования с амперсандом
            if (c == '&' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (code == 'l' || code == 'o' || code == 'n' || code == 'm' || code == 'k') {
                    currentFormats.append('§').append(code);
                } else if (code == 'r') {
                    currentFormats.setLength(0);
                }
                i++;
                continue;
            }

            // Обработка кодов форматирования со знаком параграфа
            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (code == 'l' || code == 'o' || code == 'n' || code == 'm' || code == 'k') {
                    currentFormats.append('§').append(code);
                } else if (code == 'r') {
                    currentFormats.setLength(0);
                }
                i++;
                continue;
            }

            // Интерполяция цветов
            float ratio = visibleLength > 1 ? (float) stepIndex / (float) (visibleLength - 1) : 0.0f;
            int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));

            ChatColor color = ChatColor.of(new Color(
                    Math.max(0, Math.min(255, red)),
                    Math.max(0, Math.min(255, green)),
                    Math.max(0, Math.min(255, blue))
            ));

            result.append(color).append(currentFormats).append(c);
            stepIndex++;
        }

        return result.toString();
    }

    /**
     * Создает градиент в фирменном стиле GlowTrade (#FFA500 -> #FFD700).
     */
    public static String glowGradient(String text) {
        return applyGradient(text, DEFAULT_GRADIENT_START, DEFAULT_GRADIENT_END);
    }

    /**
     * Алиас для фирменного градиента (#FFA500 -> #FFD700).
     */
    public static String atomicGradient(String text) {
        return glowGradient(text);
    }

    /**
     * Создает градиент в красных тонах (#FF5252 -> #FF1744).
     */
    public static String redGradient(String text) {
        return applyGradient(text, RED_GRADIENT_START, RED_GRADIENT_END);
    }

    /**
     * Создает градиент в зеленых тонах (#00E676 -> #1DE9B6).
     */
    public static String greenGradient(String text) {
        return applyGradient(text, GREEN_GRADIENT_START, GREEN_GRADIENT_END);
    }

    /**
     * Окрашивает название предмета GUI и гарантирует отсутствие курсива (§o),
     * чтобы предмет не выглядел как переименованный на наковальне.
     */
    public static String guiItemName(String text) {
        return "§r" + colorize(text);
    }

    /**
     * Окрашивает список строк лора без курсива наковальни.
     */
    public static List<String> guiItemLore(List<String> lore) {
        if (lore == null) return new ArrayList<>();
        List<String> colored = new ArrayList<>();
        for (String line : lore) {
            colored.add("§r" + colorize(line));
        }
        return colored;
    }
}
