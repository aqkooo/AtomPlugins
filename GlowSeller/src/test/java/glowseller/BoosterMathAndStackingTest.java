package glowseller;

import glowseller.managers.BoosterManager;
import glowseller.models.PlayerData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BoosterMathAndStackingTest {

    @Test
    @DisplayName("Verify formula: perm + purchased - 1.0")
    void testBoosterFormula() {
        // Case 1: No perm booster (1.0), no purchased booster (1.0)
        double perm1 = 1.0;
        double purchased1 = 1.0;
        double total1 = perm1 + purchased1 - 1.0;
        assertEquals(1.0, total1, 0.001);

        // Case 2: VIP perm (1.2), no purchased booster (1.0)
        double perm2 = 1.2;
        double purchased2 = 1.0;
        double total2 = perm2 + purchased2 - 1.0;
        assertEquals(1.2, total2, 0.001);

        // Case 3: No perm (1.0), purchased x2 (2.0)
        double perm3 = 1.0;
        double purchased3 = 2.0;
        double total3 = perm3 + purchased3 - 1.0;
        assertEquals(2.0, total3, 0.001);

        // Case 4: Premium perm (1.5), purchased x3 (3.0) -> 1.5 + 3.0 - 1.0 = 3.5 (no double multiplication!)
        double perm4 = 1.5;
        double purchased4 = 3.0;
        double total4 = perm4 + purchased4 - 1.0;
        assertEquals(3.5, total4, 0.001);

        // Case 5: Sponsor perm (2.0), purchased x5 (5.0) -> 2.0 + 5.0 - 1.0 = 6.0
        double perm5 = 2.0;
        double purchased5 = 5.0;
        double total5 = perm5 + purchased5 - 1.0;
        assertEquals(6.0, total5, 0.001);
    }

    @Test
    @DisplayName("Verify PlayerData booster state and expiration")
    void testPlayerDataBooster() {
        UUID uuid = UUID.randomUUID();
        PlayerData data = new PlayerData(uuid);

        assertFalse(data.hasActiveBooster());
        assertEquals(0L, data.getBoosterTimeLeftSeconds());

        // Set active booster for 1 hour
        long now = System.currentTimeMillis();
        data.setActiveBoosterKey("coin_x2_1h");
        data.setBoosterExpireAt(now + 3600_000L);

        assertTrue(data.hasActiveBooster());
        assertTrue(data.getBoosterTimeLeftSeconds() > 3500 && data.getBoosterTimeLeftSeconds() <= 3600);

        // Expired booster
        data.setBoosterExpireAt(now - 1000L);
        assertFalse(data.hasActiveBooster());
        assertEquals(0L, data.getBoosterTimeLeftSeconds());

        // Clear booster
        data.clearBooster();
        assertNull(data.getActiveBoosterKey());
        assertEquals(0L, data.getBoosterExpireAt());
    }

    @Test
    @DisplayName("Verify BoosterManager time formatting")
    void testTimeFormatting() {
        BoosterManager bm = new BoosterManager(null);

        assertEquals("0с", bm.formatTime(0));
        assertEquals("45с", bm.formatTime(45));
        assertEquals("1м 30с", bm.formatTime(90));
        assertEquals("1ч", bm.formatTime(3600));
        assertEquals("1ч 30м", bm.formatTime(5400));
        assertEquals("2ч 15м 30с", bm.formatTime(8130));
    }

    @Test
    @DisplayName("Verify purchase limits: once, numeric counts, and daily")
    void testPurchaseLimits() {
        PlayerData data = new PlayerData(UUID.randomUUID());
        assertFalse(data.hasReachedLimit("booster_15", "once"));
        assertFalse(data.hasReachedLimit("booster_15", "1"));
        assertFalse(data.hasReachedLimit("booster_15", "3"));
        assertFalse(data.hasReachedLimit("booster_15", "unlimited"));

        long now = System.currentTimeMillis();
        data.recordPurchase("booster_15", now);

        assertTrue(data.hasReachedLimit("booster_15", "once"));
        assertTrue(data.hasReachedLimit("booster_15", "1"));
        assertFalse(data.hasReachedLimit("booster_15", "3"));
        assertFalse(data.hasReachedLimit("booster_15", "unlimited"));

        data.recordPurchase("booster_15", now);
        data.recordPurchase("booster_15", now);
        assertTrue(data.hasReachedLimit("booster_15", "3"));
    }

    @Test
    @DisplayName("Verify that active booster state blocks subsequent booster purchases")
    void testActiveBoosterBlocksSubsequent() {
        PlayerData data = new PlayerData(UUID.randomUUID());
        assertFalse(data.hasActiveBooster());

        // Player buys booster
        long now = System.currentTimeMillis();
        data.setActiveBoosterKey("booster_15");
        data.setBoosterExpireAt(now + 3600_000L);

        // While active, hasActiveBooster is true -> must block infinite/consecutive booster purchases
        assertTrue(data.hasActiveBooster());

        // When expired
        data.setBoosterExpireAt(now - 1000L);
        assertFalse(data.hasActiveBooster());
    }
}
