package ru.glowdevv.glowcustomloot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;
import ru.glowdevv.glowcustomloot.util.TextUtil;

import static org.junit.jupiter.api.Assertions.*;

public class TextUtilTest {

    @Test
    void testItalicDisabledByDefault() {
        Component comp = TextUtil.parse("<green>Hello World</green>");
        assertEquals(TextDecoration.State.FALSE, comp.decoration(TextDecoration.ITALIC),
                "Italic should be explicitly set to FALSE to prevent Vanilla italic formatting");
    }

    @Test
    void testMiniMessageConversionFromLegacy() {
        String input = "&e&lTest &cMessage";
        String mm = TextUtil.convertToMiniMessage(input);
        assertTrue(mm.contains("<yellow>"), "Should convert &e to <yellow>");
        assertTrue(mm.contains("<bold>"), "Should convert &l to <bold>");
        assertTrue(mm.contains("<red>"), "Should convert &c to <red>");
    }

    @Test
    void testHexConversion() {
        String input = "&#ffaa00Colored Text";
        String mm = TextUtil.convertToMiniMessage(input);
        assertTrue(mm.contains("<#ffaa00>"), "Should convert &#ffaa00 to <#ffaa00>");
    }

    @Test
    void testToPlain() {
        Component comp = TextUtil.parse("<gold><bold>Rich Title</bold></gold>");
        String plain = TextUtil.toPlain(comp);
        assertEquals("Rich Title", plain);
    }
}
