package com.ejyqyl.glowchatgame.api.event;

import com.ejyqyl.glowchatgame.game.MathProblem;
import com.ejyqyl.glowchatgame.reward.RewardGroup;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a player solves the active chat game problem first.
 *
 * @author ejyqyl, Glowdevv
 */
public final class ChatGameWinEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final MathProblem problem;
    private final RewardGroup rewardGroup;
    private final double timeTakenSeconds;

    public ChatGameWinEvent(@NotNull Player player, @NotNull MathProblem problem,
                            @Nullable RewardGroup rewardGroup, double timeTakenSeconds) {
        this.player = player;
        this.problem = problem;
        this.rewardGroup = rewardGroup;
        this.timeTakenSeconds = timeTakenSeconds;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public MathProblem getProblem() {
        return problem;
    }

    @Nullable
    public RewardGroup getRewardGroup() {
        return rewardGroup;
    }

    public double getTimeTakenSeconds() {
        return timeTakenSeconds;
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
