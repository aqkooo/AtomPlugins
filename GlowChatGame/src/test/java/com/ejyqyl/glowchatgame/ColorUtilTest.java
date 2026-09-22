package com.ejyqyl.glowchatgame;

import com.ejyqyl.glowchatgame.util.ColorUtil;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorUtilTest {

    @Test
    void testLegacyColorCodes() {
        String input = "&aHello &cWorld &lBold";
        String colorized = ColorUtil.colorize(input);
        assertTrue(colorized.contains("§aHello"));
        assertTrue(colorized.contains("§cWorld"));
        assertTrue(colorized.contains("§lBold"));
    }

    @Test
    void testHexTags() {
        String input = "<#00B5FD>CyanText &#7670E5PurpleText";
        String colorized = ColorUtil.colorize(input);
        assertTrue(colorized.contains("CyanText"));
        assertTrue(colorized.contains("PurpleText"));
        assertTrue(colorized.contains("§x"));
    }

    @Test
    void testGradient() {
        String input = "<gradient:#00B5FD:#7670E5>GLOWCHATGAME</gradient>";
        String colorized = ColorUtil.colorize(input);
        String stripped = ChatColor.stripColor(colorized);
        assertEquals("GLOWCHATGAME", stripped);
        assertTrue(colorized.contains("§x"));
    }

    @Test
    void testRainbow() {
        String input = "<rainbow>RAINBOW TEXT</rainbow>";
        String colorized = ColorUtil.colorize(input);
        String stripped = ChatColor.stripColor(colorized);
        assertEquals("RAINBOW TEXT", stripped);
        assertTrue(colorized.contains("§x"));
    }

    @Test
    void testNamedTags() {
        String input = "<bold><yellow>Caution</yellow></bold>";
        String colorized = ColorUtil.colorize(input);
        assertTrue(colorized.contains("§l"));
        assertTrue(colorized.contains("§eCaution"));
    }

    @Test
    void testGradientWithEmbeddedBold() {
        String input = "<gradient:#00B5FD:#7670E5>&l✦ GLOWCHATGAME ПОМОЩЬ ✦</gradient>";
        String colorized = ColorUtil.colorize(input);
        assertFalse(colorized.contains("&l"), "Raw &l must not appear in output");
        assertTrue(colorized.contains("§l"), "Output must contain bold formatting code");
        String stripped = ChatColor.stripColor(colorized);
        assertEquals("✦ GLOWCHATGAME ПОМОЩЬ ✦", stripped);
    }
}
