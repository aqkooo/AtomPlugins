package com.ejyqyl.atomduels.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BetCalculationTest {

    @Test
    @DisplayName("Should correctly calculate 5% burn fee and winner payout")
    void testFeeAndPayoutCalculation() {
        double bet = 1000.0;
        double feePercent = 5.0;

        double totalPot = bet * 2.0;
        assertEquals(2000.0, totalPot);

        double fee = totalPot * (feePercent / 100.0);
        assertEquals(100.0, fee);

        double winnerPrize = totalPot - fee;
        assertEquals(1900.0, winnerPrize);

        double netProfit = winnerPrize - bet;
        assertEquals(900.0, netProfit);
    }

    @Test
    @DisplayName("Should handle 0$ stake without fees")
    void testZeroStake() {
        double bet = 0.0;
        double feePercent = 5.0;

        double totalPot = bet * 2.0;
        double fee = totalPot * (feePercent / 100.0);
        double winnerPrize = totalPot - fee;

        assertEquals(0.0, totalPot);
        assertEquals(0.0, fee);
        assertEquals(0.0, winnerPrize);
    }
}
