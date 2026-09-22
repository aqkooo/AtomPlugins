package com.ejyqyl.atomduels.util;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorUtilTest {

    @Test
    @DisplayName("Should convert text to small caps properly")
    void testSmallCapsConversion() {
        String input = "Hello World";
        String smallCaps = ColorUtil.toSmallCaps(input);
        assertEquals("ʜᴇʟʟᴏ ᴡᴏʀʟᴅ", smallCaps);
    }

    @Test
    @DisplayName("Should parse legacy color codes into Adventure components")
    void testLegacyParsing() {
        Component comp = ColorUtil.parse("&aHello &eWorld");
        assertNotNull(comp);
    }

    @Test
    @DisplayName("Should parse miniMessage gradient tags into Adventure components")
    void testGradientParsing() {
        Component comp = ColorUtil.parse("<gradient:#00B5FD:#7670E5>AtomDuels</gradient>");
        assertNotNull(comp);
    }
}
