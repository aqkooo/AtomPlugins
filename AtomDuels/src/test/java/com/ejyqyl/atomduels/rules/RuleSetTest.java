package com.ejyqyl.atomduels.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleSetTest {

    @Test
    @DisplayName("Should correctly validate rounds and wins needed")
    void testRoundsValidation() {
        RuleSet rs = new RuleSet();
        rs.setMaxRounds(1);
        assertEquals(1, rs.getMaxRounds());
        assertEquals(1, rs.getRoundsToWin());

        rs.setMaxRounds(3);
        assertEquals(3, rs.getMaxRounds());
        assertEquals(2, rs.getRoundsToWin());

        rs.setMaxRounds(5);
        assertEquals(5, rs.getMaxRounds());
        assertEquals(3, rs.getRoundsToWin());

        // Invalid round count fallback
        rs.setMaxRounds(4);
        assertEquals(1, rs.getMaxRounds());
    }

    @Test
    @DisplayName("Should correctly toggle and track rules")
    void testRuleToggles() {
        RuleSet rs = new RuleSet();
        assertTrue(rs.isRuleEnabled(Rule.SWORD));

        rs.toggleRule(Rule.SWORD);
        assertFalse(rs.isRuleEnabled(Rule.SWORD));

        rs.setRule(Rule.SWORD, true);
        assertTrue(rs.isRuleEnabled(Rule.SWORD));
    }

    @Test
    @DisplayName("Should properly copy rule sets independently")
    void testRuleSetCopy() {
        RuleSet original = new RuleSet();
        original.setKitName("Mace");
        original.setBetAmount(500.0);
        original.setRule(Rule.BOW, false);

        RuleSet copy = original.copy();
        assertEquals("Mace", copy.getKitName());
        assertEquals(500.0, copy.getBetAmount());
        assertFalse(copy.isRuleEnabled(Rule.BOW));

        copy.setBetAmount(1000.0);
        assertEquals(500.0, original.getBetAmount(), "Original must not mutate when copy changes");
    }
}
