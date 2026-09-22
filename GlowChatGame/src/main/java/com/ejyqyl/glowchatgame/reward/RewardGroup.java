package com.ejyqyl.glowchatgame.reward;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Container for a chance-based reward tier.
 *
 * @author ejyqyl, Glowdevv
 */
public final class RewardGroup {

    private final int chance;
    private final String displayName;
    private final List<String> commands;

    public RewardGroup(int chance, @NotNull String displayName, @NotNull List<String> commands) {
        this.chance = chance;
        this.displayName = displayName;
        this.commands = commands;
    }

    public int getChance() {
        return chance;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public List<String> getCommands() {
        return Collections.unmodifiableList(commands);
    }
}
