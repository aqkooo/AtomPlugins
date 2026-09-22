package ru.glowdevv.glowcustomloot.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum LootMode {
    MERGE("Добавить к ванильному"),
    REPLACE("Заменить ванильный");

    private final String description;

    LootMode(String description) {
        this.description = description;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    public static LootMode fromString(@Nullable String input) {
        if (input != null && input.equalsIgnoreCase("REPLACE")) {
            return REPLACE;
        }
        return MERGE;
    }

    @NotNull
    public LootMode toggle() {
        return this == MERGE ? REPLACE : MERGE;
    }
}
