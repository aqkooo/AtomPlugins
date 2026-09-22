package com.ejyqyl.glowchatgame.game;

import com.ejyqyl.glowchatgame.config.ConfigManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Intelligent generator for arithmetic problems.
 * Ensures clean numbers, non-negative subtraction results where appropriate,
 * and guaranteed integer division without fractions.
 *
 * @author ejyqyl, Glowdevv
 */
public final class MathProblemGenerator {

    private final ConfigManager configManager;

    public MathProblemGenerator(@NotNull ConfigManager configManager) {
        this.configManager = configManager;
    }

    @NotNull
    public MathProblem generate(@NotNull Difficulty difficulty) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        int min = configManager.getMinNumber(difficulty);
        int max = configManager.getMaxNumber(difficulty);
        if (min <= 0) min = 1;
        if (max < min) max = min + 10;

        List<MathOperation> allowed = getAvailableOperations(difficulty);
        if (allowed.isEmpty()) {
            allowed = List.of(MathOperation.ADD);
        }

        boolean allowMultiStep = configManager.isMultiStepAllowed(difficulty);
        int multiStepChance = configManager.getMultiStepChance(difficulty);

        if (allowMultiStep && random.nextInt(100) < multiStepChance) {
            return generateMultiStep(difficulty, min, max, allowed, random);
        } else {
            return generateSingleStep(difficulty, min, max, allowed, random);
        }
    }

    @NotNull
    private MathProblem generateSingleStep(@NotNull Difficulty difficulty, int min, int max,
                                           @NotNull List<MathOperation> allowed, @NotNull ThreadLocalRandom random) {
        MathOperation op = allowed.get(random.nextInt(allowed.size()));

        long a;
        long b;
        long answer;
        String expression;

        switch (op) {
            case ADD:
                a = random.nextLong(min, max + 1);
                b = random.nextLong(min, max + 1);
                answer = a + b;
                expression = a + " + " + b;
                break;
            case SUBTRACT:
                long num1 = random.nextLong(min, max + 1);
                long num2 = random.nextLong(min, max + 1);
                a = Math.max(num1, num2);
                b = Math.min(num1, num2);
                answer = a - b;
                expression = a + " - " + b;
                break;
            case MULTIPLY:
                int multMax = (int) Math.min(max, difficulty == Difficulty.HARD ? 50 : (difficulty == Difficulty.MEDIUM ? 20 : 10));
                int multMin = Math.min(min, multMax);
                a = random.nextLong(Math.max(1, multMin), multMax + 1);
                b = random.nextLong(Math.max(1, multMin), multMax + 1);
                answer = a * b;
                expression = a + " × " + b;
                break;
            case DIVIDE:
            default:
                int divQuotientMax = (int) Math.min(max, difficulty == Difficulty.HARD ? 50 : 20);
                int divDivisorMax = (int) Math.min(12, Math.max(2, max / 2));
                long quotient = random.nextLong(Math.max(1, min), divQuotientMax + 1);
                long divisor = random.nextLong(2, divDivisorMax + 1);
                long dividend = quotient * divisor;
                answer = quotient;
                expression = dividend + " ÷ " + divisor;
                break;
        }

        return new MathProblem(expression, answer, difficulty);
    }

    @NotNull
    private MathProblem generateMultiStep(@NotNull Difficulty difficulty, int min, int max,
                                          @NotNull List<MathOperation> allowed, @NotNull ThreadLocalRandom random) {
        int pattern = random.nextInt(3);

        if (pattern == 0 && allowed.contains(MathOperation.ADD) && allowed.contains(MathOperation.MULTIPLY)) {
            // (a + b) × c
            long a = random.nextLong(1, 15);
            long b = random.nextLong(1, 15);
            long c = random.nextLong(2, 6);
            long answer = (a + b) * c;
            return new MathProblem("(" + a + " + " + b + ") × " + c, answer, difficulty);
        } else if (pattern == 1 && allowed.contains(MathOperation.MULTIPLY) && allowed.contains(MathOperation.SUBTRACT)) {
            // a × b - c
            long a = random.nextLong(2, 12);
            long b = random.nextLong(2, 12);
            long product = a * b;
            long c = random.nextLong(1, product);
            long answer = product - c;
            return new MathProblem(a + " × " + b + " - " + c, answer, difficulty);
        } else {
            // a + b + c
            long a = random.nextLong(min, max + 1);
            long b = random.nextLong(min, max + 1);
            long c = random.nextLong(min, max + 1);
            long answer = a + b + c;
            return new MathProblem(a + " + " + b + " + " + c, answer, difficulty);
        }
    }

    @NotNull
    private List<MathOperation> getAvailableOperations(@NotNull Difficulty difficulty) {
        List<MathOperation> available = new ArrayList<>();
        List<MathOperation> diffOps = configManager.getOperationsForDifficulty(difficulty);

        for (MathOperation op : diffOps) {
            if (configManager.isOperationGloballyEnabled(op)) {
                available.add(op);
            }
        }

        if (available.isEmpty()) {
            available.add(MathOperation.ADD);
        }
        return available;
    }
}
