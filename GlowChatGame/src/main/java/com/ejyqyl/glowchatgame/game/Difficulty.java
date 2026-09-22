package com.ejyqyl.glowchatgame.game;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Math game difficulty levels.
 *
 * @author ejyqyl, Glowdevv
 */
public enum Difficulty {
    EASY("Легкий"),
    MEDIUM("Средний"),
    HARD("Сложный");

    private final String displayName;

    Difficulty(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public static Difficulty fromString(@Nullable String input, @NotNull Difficulty fallback) {
        if (input == null || input.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Difficulty.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
