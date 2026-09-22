package com.ejyqyl.glowchatgame.game;

import org.jetbrains.annotations.NotNull;

/**
 * Immutable model representing a generated math problem and its expected answer.
 *
 * @author ejyqyl, Glowdevv
 */
public final class MathProblem {

    private final String expression;
    private final long answer;
    private final Difficulty difficulty;

    public MathProblem(@NotNull String expression, long answer, @NotNull Difficulty difficulty) {
        this.expression = expression;
        this.answer = answer;
        this.difficulty = difficulty;
    }

    @NotNull
    public String getExpression() {
        return expression;
    }

    public long getAnswer() {
        return answer;
    }

    @NotNull
    public Difficulty getDifficulty() {
        return difficulty;
    }

    /**
     * Checks if the provided user input matches the expected numerical answer.
     *
     * @param input Raw text from chat
     * @return true if input parses as long and equals expected answer
     */
    public boolean checkAnswer(@NotNull String input) {
        String trimmed = input.trim();
        try {
            long parsed = Long.parseLong(trimmed);
            return parsed == answer;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @Override
    public String toString() {
        return expression + " = " + answer + " (" + difficulty + ")";
    }
}
