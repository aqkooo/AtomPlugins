package com.ejyqyl.glowchatgame.api.event;

import com.ejyqyl.glowchatgame.game.MathProblem;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when no player answered the math game in time.
 *
 * @author ejyqyl, Glowdevv
 */
public final class ChatGameTimeoutEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final MathProblem problem;

    public ChatGameTimeoutEvent(@NotNull MathProblem problem) {
        this.problem = problem;
    }

    @NotNull
    public MathProblem getProblem() {
        return problem;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
