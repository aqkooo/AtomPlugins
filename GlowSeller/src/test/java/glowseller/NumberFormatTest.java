package glowseller;

import glowseller.managers.NumberFormatManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NumberFormatTest {

    @Test
    @DisplayName("Verify NumberFormatManager standard and compact formats")
    void testNumberFormatting() {
        NumberFormatManager nfm = new NumberFormatManager();

        // Standard formatting
        assertEquals("500", nfm.formatNumber(500L));
        assertEquals("1,000", nfm.formatNumber(1000L));
        assertEquals("1,500,000", nfm.formatNumber(1500000L));

        // Compact formatting
        assertEquals("500", nfm.formatCompact(500.0));
        assertEquals("1.5K", nfm.formatCompact(1500.0));
        assertEquals("2.5M", nfm.formatCompact(2500000.0));
        assertEquals("10B", nfm.formatCompact(10000000000.0));
    }
}
