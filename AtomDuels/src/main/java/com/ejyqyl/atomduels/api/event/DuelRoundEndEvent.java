package com.ejyqyl.atomduels.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Triggered when a single round inside a match ends.
 * Authored by ejyqyl.
 */
public class DuelRoundEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID duelId;
    private final int roundNumber;
    private final Player roundWinner;

    public DuelRoundEndEvent(UUID duelId, int roundNumber, Player roundWinner) {
        this.duelId = duelId;
        this.roundNumber = roundNumber;
        this.roundWinner = roundWinner;
    }

    public UUID getDuelId() {
        return duelId;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public Player getRoundWinner() {
        return roundWinner;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
