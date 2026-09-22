package com.ejyqyl.atomduels.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Triggered when a duel ends with a winner or draw.
 * Authored by ejyqyl.
 */
public class DuelEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID duelId;
    private final Player winner;
    private final Player loser;
    private final boolean isDraw;
    private final double betAmount;

    public DuelEndEvent(UUID duelId, Player winner, Player loser, boolean isDraw, double betAmount) {
        this.duelId = duelId;
        this.winner = winner;
        this.loser = loser;
        this.isDraw = isDraw;
        this.betAmount = betAmount;
    }

    public UUID getDuelId() {
        return duelId;
    }

    public Player getWinner() {
        return winner;
    }

    public Player getLoser() {
        return loser;
    }

    public boolean isDraw() {
        return isDraw;
    }

    public double getBetAmount() {
        return betAmount;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
