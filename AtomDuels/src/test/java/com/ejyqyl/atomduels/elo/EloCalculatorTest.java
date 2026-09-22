package com.ejyqyl.atomduels.elo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EloCalculatorTest {

    @Test
    @DisplayName("Should strictly enforce zero-sum delta changes between players")
    void testZeroSumGuarantee() {
        EloCalculator.EloResult resWin = EloCalculator.calculate(1000, 1000, 20, 20, 1.0);
        assertEquals(0, resWin.deltaA() + resWin.deltaB(), "Deltas must sum to exactly zero");
        assertTrue(resWin.deltaA() > 0, "Winner must gain rating");
        assertTrue(resWin.deltaB() < 0, "Loser must lose rating");

        EloCalculator.EloResult resDraw = EloCalculator.calculate(1200, 1200, 20, 20, 0.5);
        assertEquals(0, resDraw.deltaA() + resDraw.deltaB(), "Draw deltas must sum to zero");
    }

    @Test
    @DisplayName("Should apply higher K-factor during calibration period (first 10 matches)")
    void testCalibrationKFactor() {
        int kCalib = EloCalculator.getKFactor(1000, 3);
        int kNormal = EloCalculator.getKFactor(1000, 25);
        int kHighElo = EloCalculator.getKFactor(1900, 50);

        assertEquals(40, kCalib, "Calibration K should be 40");
        assertEquals(24, kNormal, "Normal K should be 24");
        assertEquals(16, kHighElo, "High Elo K should be 16");
    }

    @Test
    @DisplayName("Should never let Elo fall below minimum floor")
    void testMinimumEloFloor() {
        EloCalculator.EloResult res = EloCalculator.calculate(100, 2000, 50, 50, 0.0);
        assertTrue(res.newRatingA() >= 100, "Elo must never drop below 100");
    }

    @Test
    @DisplayName("Should match expected Elo formula probabilities")
    void testExpectedProbabilities() {
        double expectedEqual = EloCalculator.calculateExpectedScore(1000, 1000);
        assertEquals(0.5, expectedEqual, 0.001);

        double expectedBetter = EloCalculator.calculateExpectedScore(1400, 1000);
        assertTrue(expectedBetter > 0.85, "Higher rated player should have high expected win score");
    }
}
