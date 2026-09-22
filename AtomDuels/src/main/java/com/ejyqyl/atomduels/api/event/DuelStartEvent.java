package com.ejyqyl.atomduels.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Triggered when a duel begins its countdown or first round.
 * Authored by ejyqyl.
 */
public class DuelStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID duelId;
    private final Player player1;
    private final Player player2;
    private final boolean isBot;
    private final String arenaName;
    private final String kitName;

    public DuelStartEvent(UUID duelId, Player player1, Player player2, boolean isBot, String arenaName, String kitName) {
        this.duelId = duelId;
        this.player1 = player1;
        this.player2 = player2;
        this.isBot = isBot;
        this.arenaName = arenaName;
        this.kitName = kitName;
    }

    public UUID getDuelId() {
        return duelId;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public boolean isBot() {
        return isBot;
    }

    public String getArenaName() {
        return arenaName;
    }

    public String getKitName() {
        return kitName;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
