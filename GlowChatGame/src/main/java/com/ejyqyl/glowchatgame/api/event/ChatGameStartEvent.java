package com.ejyqyl.glowchatgame.api.event;

import com.ejyqyl.glowchatgame.game.MathProblem;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired right before a chat math game starts. Can be cancelled.
 *
 * @author ejyqyl, Glowdevv
 */
public final class ChatGameStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final MathProblem problem;
    private boolean cancelled = false;

    public ChatGameStartEvent(@NotNull MathProblem problem) {
        this.problem = problem;
    }

    @NotNull
    public MathProblem getProblem() {
        return problem;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
