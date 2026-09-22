package com.ejyqyl.glowcmd;

import com.ejyqyl.glowcmd.util.ColorUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorUtilTest {

    @Test
    void testNullAndEmpty() {
        assertNull(ColorUtil.colorize(null));
        assertEquals("", ColorUtil.colorize(""));
    }

    @Test
    void testLegacyColorCodes() {
        String input = "&aHello &cWorld &lBold";
        String colorized = ColorUtil.colorize(input);

        assertNotNull(colorized);
        assertTrue(colorized.contains("§a"));
        assertTrue(colorized.contains("§c"));
        assertTrue(colorized.contains("§l"));
        assertFalse(colorized.contains("&a"));
    }

    @Test
    void testHexColorCodes() {
        if (!ColorUtil.isHexSupported()) {
            return;
        }

        String input = "&#00FFAASpawn set!";
        String colorized = ColorUtil.colorize(input);

        assertNotNull(colorized);
        assertFalse(colorized.contains("&#00FFAA"));
        assertTrue(colorized.contains("Spawn set!"));
    }

    @Test
    void testMixedColors() {
        String input = "&8[&bGlowCMD&8] &#00FFAASuccess!";
        String colorized = ColorUtil.colorize(input);

        assertNotNull(colorized);
        assertTrue(colorized.contains("§8"));
        assertTrue(colorized.contains("§b"));
        assertTrue(colorized.contains("Success!"));
    }

    @Test
    void testGradientSupport() {
        String input = "<gradient:#00B5FD:#7670E5>ɢʟᴏᴡᴄᴍᴅ ≫</gradient> <gradient:#BC00FD:#7670E5>У вас недостаточно прав.</gradient>";
        String colorized = ColorUtil.colorize(input);

        assertNotNull(colorized);
        assertFalse(colorized.contains("<gradient:"));
        assertTrue(colorized.contains("§x§0§0§b§5§f§d")); // First color of gradient
        String stripped = org.bukkit.ChatColor.stripColor(colorized);
        assertEquals("ɢʟᴏᴡᴄᴍᴅ ≫ У вас недостаточно прав.", stripped.trim());
    }
}
