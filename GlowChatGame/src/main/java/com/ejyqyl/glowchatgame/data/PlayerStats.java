package com.ejyqyl.glowchatgame.data;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.UUID;

/**
 * POJO holding individual player statistics for GlowChatGame.
 *
 * @author ejyqyl, Glowdevv
 */
public final class PlayerStats {

    private final UUID uuid;
    private String playerName;
    private int wins;
    private int games;
    private int rewardsClaimed;
    private long lastWinTimestamp;

    public PlayerStats(@NotNull UUID uuid, @NotNull String playerName, int wins, int games, int rewardsClaimed, long lastWinTimestamp) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.wins = wins;
        this.games = games;
        this.rewardsClaimed = rewardsClaimed;
        this.lastWinTimestamp = lastWinTimestamp;
    }

    public PlayerStats(@NotNull UUID uuid, @NotNull String playerName) {
        this(uuid, playerName, 0, 0, 0, 0L);
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    @NotNull
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(@NotNull String playerName) {
        this.playerName = playerName;
    }

    public int getWins() {
        return wins;
    }

    public void incrementWins() {
        this.wins++;
    }

    public int getGames() {
        return games;
    }

    public void incrementGames() {
        this.games++;
    }

    public int getRewardsClaimed() {
        return rewardsClaimed;
    }

    public void incrementRewardsClaimed() {
        this.rewardsClaimed++;
    }

    public long getLastWinTimestamp() {
        return lastWinTimestamp;
    }

    public void setLastWinTimestamp(long lastWinTimestamp) {
        this.lastWinTimestamp = lastWinTimestamp;
    }

    public double getAccuracy() {
        if (games <= 0) {
            return 0.0;
        }
        return ((double) wins / (double) games) * 100.0;
    }

    @NotNull
    public String getFormattedAccuracy() {
        return String.format(Locale.ROOT, "%.1f", getAccuracy());
    }
}
