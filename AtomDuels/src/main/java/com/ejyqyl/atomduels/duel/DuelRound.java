package com.ejyqyl.atomduels.duel;

import java.util.UUID;

/**
 * Record of a single round in a match.
 * Authored by ejyqyl.
 */
public class DuelRound {

    private final int roundNumber;
    private UUID winnerUuid;
    private int durationSeconds;

    public DuelRound(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public UUID getWinnerUuid() {
        return winnerUuid;
    }

    public void setWinnerUuid(UUID winnerUuid) {
        this.winnerUuid = winnerUuid;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
