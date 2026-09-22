package com.ejyqyl.atomduels.duel;

/**
 * Lifecycle states of an ActiveDuel.
 * Authored by ejyqyl.
 */
public enum DuelState {
    WAITING_CONFIRMATION,
    COUNTDOWN,
    IN_FIGHT,
    ROUND_RESET,
    ENDED
}
