package com.ejyqyl.glowchatgame;

import com.ejyqyl.glowchatgame.data.PlayerStats;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerStatsTest {

    @Test
    void testStatsAccuracyCalculation() {
        PlayerStats stats = new PlayerStats(UUID.randomUUID(), "Steve");
        assertEquals(0, stats.getWins());
        assertEquals(0, stats.getGames());
        assertEquals(0.0, stats.getAccuracy());
        assertEquals("0.0", stats.getFormattedAccuracy());

        // 15 wins out of 42 games
        for (int i = 0; i < 42; i++) {
            stats.incrementGames();
        }
        for (int i = 0; i < 15; i++) {
            stats.incrementWins();
        }

        assertEquals(15, stats.getWins());
        assertEquals(42, stats.getGames());
        assertEquals("35.7", stats.getFormattedAccuracy());
    }
}
