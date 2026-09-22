package com.ejyqyl.glowchatgame.game;

import com.ejyqyl.glowchatgame.reward.RewardGroup;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Encapsulates the active state of a running chat game.
 * Uses atomic variables to guarantee single-winner concurrency safety.
 *
 * @author ejyqyl, Glowdevv
 */
public final class ActiveGame {

    private final MathProblem problem;
    private final RewardGroup rewardGroup;
    private final long startTime;
    private final int timeoutSeconds;
    private final AtomicBoolean solved = new AtomicBoolean(false);

    private BukkitTask timeoutTask;
    private Player winner;
    private double timeTakenSeconds;

    public ActiveGame(@NotNull MathProblem problem, @Nullable RewardGroup rewardGroup, int timeoutSeconds) {
        this.problem = problem;
        this.rewardGroup = rewardGroup;
        this.startTime = System.currentTimeMillis();
        this.timeoutSeconds = timeoutSeconds;
    }

    @NotNull
    public MathProblem getProblem() {
        return problem;
    }

    @Nullable
    public RewardGroup getRewardGroup() {
        return rewardGroup;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean isSolved() {
        return solved.get();
    }

    public void setTimeoutTask(@Nullable BukkitTask task) {
        this.timeoutTask = task;
    }

    public void cancelTimeoutTask() {
        if (timeoutTask != null) {
            timeoutTask.cancel();
            this.timeoutTask = null;
        }
    }

    /**
     * Atomically attempts to claim the victory for the specified player and message.
     *
     * @param player  The player submitting the chat message
     * @param message Raw text message
     * @return true if the answer is correct and this thread claimed the win
     */
    public boolean trySolve(@NotNull Player player, @NotNull String message) {
        if (solved.get()) {
            return false;
        }

        if (!problem.checkAnswer(message)) {
            return false;
        }

        if (solved.compareAndSet(false, true)) {
            this.winner = player;
            this.timeTakenSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
            cancelTimeoutTask();
            return true;
        }

        return false;
    }

    /**
     * Atomically forces completion (e.g. upon timeout or administrative stop).
     *
     * @return true if game was successfully stopped from active state
     */
    public boolean forceStop() {
        cancelTimeoutTask();
        return solved.compareAndSet(false, true);
    }

    @Nullable
    public Player getWinner() {
        return winner;
    }

    public double getTimeTakenSeconds() {
        return timeTakenSeconds;
    }

    public double getTimeRemainingSeconds() {
        long elapsed = System.currentTimeMillis() - startTime;
        double remaining = timeoutSeconds - (elapsed / 1000.0);
        return Math.max(0.0, remaining);
    }
}
