package com.ejyqyl.glowchatgame.game;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Supported arithmetic operations.
 *
 * @author ejyqyl, Glowdevv
 */
public enum MathOperation {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("×"),
    DIVIDE("÷");

    private final String symbol;

    MathOperation(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    @NotNull
    public static MathOperation fromString(@NotNull String name) {
        try {
            return MathOperation.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ADD;
        }
    }
}
