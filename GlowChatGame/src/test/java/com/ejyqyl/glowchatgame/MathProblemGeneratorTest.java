package com.ejyqyl.glowchatgame;

import com.ejyqyl.glowchatgame.config.ConfigManager;
import com.ejyqyl.glowchatgame.game.Difficulty;
import com.ejyqyl.glowchatgame.game.MathOperation;
import com.ejyqyl.glowchatgame.game.MathProblem;
import com.ejyqyl.glowchatgame.game.MathProblemGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MathProblemGeneratorTest {

    private ConfigManager configManager;
    private MathProblemGenerator generator;

    @BeforeEach
    void setUp() {
        configManager = Mockito.mock(ConfigManager.class);
        when(configManager.getMinNumber(any())).thenReturn(1);
        when(configManager.getMaxNumber(any())).thenReturn(50);
        when(configManager.isOperationGloballyEnabled(any())).thenReturn(true);
        when(configManager.isDivisionIntegerOnly()).thenReturn(true);
        when(configManager.getOperationsForDifficulty(any())).thenReturn(List.of(MathOperation.values()));
        generator = new MathProblemGenerator(configManager);
    }

    @Test
    void testCheckAnswer() {
        MathProblem problem = new MathProblem("37 + 18", 55, Difficulty.EASY);
        assertTrue(problem.checkAnswer("55"));
        assertTrue(problem.checkAnswer("  55  "));
        assertFalse(problem.checkAnswer("54"));
        assertFalse(problem.checkAnswer("not_a_number"));
    }

    @Test
    void testDivisionProblemsAlwaysCleanIntegers() {
        when(configManager.getOperationsForDifficulty(any())).thenReturn(List.of(MathOperation.DIVIDE));

        for (int i = 0; i < 50; i++) {
            MathProblem problem = generator.generate(Difficulty.EASY);
            assertTrue(problem.getExpression().contains("÷"));
            assertTrue(problem.getAnswer() > 0);

            // Verify expression format "A ÷ B"
            String[] parts = problem.getExpression().split(" ÷ ");
            assertEquals(2, parts.length);
            long a = Long.parseLong(parts[0].trim());
            long b = Long.parseLong(parts[1].trim());

            assertEquals(0, a % b, "Division must leave 0 remainder");
            assertEquals(problem.getAnswer(), a / b, "Answer must equal quotient");
        }
    }

    @Test
    void testDifficultyRanges() {
        when(configManager.getMinNumber(Difficulty.EASY)).thenReturn(1);
        when(configManager.getMaxNumber(Difficulty.EASY)).thenReturn(20);

        for (int i = 0; i < 25; i++) {
            MathProblem problem = generator.generate(Difficulty.EASY);
            assertNotNull(problem.getExpression());
            assertTrue(problem.getAnswer() >= 0);
        }
    }
}
