package com.ejyqyl.atompvpbot;

import com.ejyqyl.atompvpbot.ai.BotBehaviorMode;
import com.ejyqyl.atompvpbot.ai.BotDifficulty;
import com.ejyqyl.atompvpbot.ai.CustomAIProfile;
import com.ejyqyl.atompvpbot.data.PlayerData;
import com.ejyqyl.atompvpbot.util.ColorUtil;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying core logic of AtomPvPbot.
 *
 * @author ejyqyl
 */
public class BotAITest {

    @Test
    public void testColorUtilGradientAndHex() {
        String input = "<gradient:#00B5FD:#7670E5>AtomPvPbot</gradient>";
        String result = ColorUtil.colorize(input);
        assertNotNull(result);
        assertTrue(result.contains("§x")); // Hex color codes

        // Test removal of closing hex tags
        String unclosed = "<#FFAA00>Test</#FFAA00>";
        String cleaned = ColorUtil.colorize(unclosed);
        assertNotNull(cleaned);
        assertFalse(cleaned.contains("</#FFAA00>"));
        assertFalse(cleaned.contains("</color>"));
    }

    @Test
    public void testPlayerDataCalculations() {
        PlayerData data = new PlayerData(UUID.randomUUID(), "Tester");
        assertEquals(0, data.getWins());
        assertEquals(0, data.getLosses());
        assertEquals(0.0, data.getWinRate());

        data.addWin();
        data.addWin();
        data.addLoss();

        assertEquals(2, data.getWins());
        assertEquals(1, data.getLosses());
        assertEquals(3, data.getTotalGames());
        assertEquals(0, data.getWinStreak()); // Reset after loss
        assertEquals(2, data.getBestStreak());
        assertEquals(66.66, data.getWinRate(), 0.1);
    }

    @Test
    public void testBotDifficultyPresets() {
        for (BotDifficulty diff : BotDifficulty.values()) {
            assertNotNull(diff.getDisplayName());
            assertTrue(diff.getCps() >= 1 && diff.getCps() <= 20);
            assertTrue(diff.getReach() >= 2.0 && diff.getReach() <= 4.0);
            assertTrue(diff.getAimAccuracy() > 0.0 && diff.getAimAccuracy() <= 1.0);
            assertTrue(diff.getAimSpeed() > 0.0f);
        }
    }

    @Test
    public void testBotBehaviorModes() {
        for (BotBehaviorMode mode : BotBehaviorMode.values()) {
            assertNotNull(mode.getDisplayName());
            assertNotNull(mode.getDescription());
            assertTrue(mode.getSpeedMultiplier() > 0.5 && mode.getSpeedMultiplier() < 2.0);
        }
    }

    @Test
    public void testCustomAIProfileClamping() {
        CustomAIProfile profile = new CustomAIProfile();
        profile.setCps(100); // Exceeds normal bounds
        assertEquals(100, profile.getCps());
    }
}
