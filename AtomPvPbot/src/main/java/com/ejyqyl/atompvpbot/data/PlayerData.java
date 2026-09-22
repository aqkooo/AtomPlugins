package com.ejyqyl.atompvpbot.data;

import java.util.UUID;

/**
 * Player statistics and combat profile in AtomPvPbot.
 *
 * @author ejyqyl
 */
public class PlayerData {

    private final UUID uuid;
    private String username;
    private int wins = 0;
    private int losses = 0;
    private int winStreak = 0;
    private int bestStreak = 0;
    private int totalGames = 0;

    public PlayerData(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int getWinStreak() {
        return winStreak;
    }

    public void setWinStreak(int winStreak) {
        this.winStreak = winStreak;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = bestStreak;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(int totalGames) {
        this.totalGames = totalGames;
    }

    public void addWin() {
        this.wins++;
        this.totalGames++;
        this.winStreak++;
        if (winStreak > bestStreak) {
            this.bestStreak = winStreak;
        }
    }

    public void addLoss() {
        this.losses++;
        this.totalGames++;
        this.winStreak = 0;
    }

    public double getWinRate() {
        if (totalGames == 0) return 0.0;
        return ((double) wins / totalGames) * 100.0;
    }
}
