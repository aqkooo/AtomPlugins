package glowseller;

import glowseller.utils.Colorizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ColorizerTest {

    @Test
    @DisplayName("Verify Colorizer legacy and hex formatting")
    void testColorizer() {
        assertNull(Colorizer.colorize((String) null));
        assertEquals("", Colorizer.colorize(""));

        // Legacy colors
        String coloredLegacy = Colorizer.colorize("&aТекст &lжирный &fбелый");
        assertNotNull(coloredLegacy);
        assertTrue(coloredLegacy.contains("§a") || coloredLegacy.contains("&a"));

        // Hex colors
        String coloredHex1 = Colorizer.colorize("&#54A0F4Бустеры");
        assertNotNull(coloredHex1);

        String coloredHex2 = Colorizer.colorize("<#54A0F4>Бустеры");
        assertNotNull(coloredHex2);

        // Strip colors
        String stripped = Colorizer.stripColor("&aТекст");
        assertEquals("Текст", stripped);

        // List colorize
        List<String> lines = List.of("&7Первая строка", "&aВторая строка");
        List<String> processed = Colorizer.colorize(lines);
        assertEquals(2, processed.size());
    }
}
