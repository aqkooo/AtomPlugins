package com.ejyqyl.atomduels.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Triggered when bets are about to be frozen in escrow.
 * Authored by ejyqyl.
 */
public class DuelBetEscrowEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID duelId;
    private final UUID player1;
    private final UUID player2;
    private double betAmount;
    private boolean cancelled;

    public DuelBetEscrowEvent(UUID duelId, UUID player1, UUID player2, double betAmount) {
        this.duelId = duelId;
        this.player1 = player1;
        this.player2 = player2;
        this.betAmount = betAmount;
    }

    public UUID getDuelId() {
        return duelId;
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public double getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(double betAmount) {
        this.betAmount = betAmount;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
