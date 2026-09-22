package com.ejyqyl.atomduels.data;

import com.ejyqyl.atomduels.elo.EloRank;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-memory representation of player statistics and claim/loot storages.
 * Authored by ejyqyl.
 */
public class PlayerData {

    private final UUID uuid;
    private String username;
    private int elo = 1000;
    private int wins = 0;
    private int losses = 0;
    private int draws = 0;
    private int winStreak = 0;
    private int bestStreak = 0;
    private int calibrationMatches = 0;
    private double moneyWon = 0.0;
    private double moneyLost = 0.0;
    private final List<ItemStack> claimItems = new ArrayList<>();
    private final List<ItemStack> loserLootItems = new ArrayList<>();

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

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = Math.max(100, elo);
    }

    public EloRank getRank() {
        return EloRank.fromElo(elo);
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public void incrementWins() {
        this.wins++;
        this.winStreak++;
        if (this.winStreak > this.bestStreak) {
            this.bestStreak = this.winStreak;
        }
        this.calibrationMatches++;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public void incrementLosses() {
        this.losses++;
        this.winStreak = 0;
        this.calibrationMatches++;
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }

    public void incrementDraws() {
        this.draws++;
        this.calibrationMatches++;
    }

    public int getTotalMatches() {
        return wins + losses + draws;
    }

    public double getWinRate() {
        int total = getTotalMatches();
        if (total == 0) return 0.0;
        return (double) wins / total * 100.0;
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

    public int getCalibrationMatches() {
        return calibrationMatches;
    }

    public void setCalibrationMatches(int calibrationMatches) {
        this.calibrationMatches = calibrationMatches;
    }

    public double getMoneyWon() {
        return moneyWon;
    }

    public void setMoneyWon(double moneyWon) {
        this.moneyWon = moneyWon;
    }

    public void addMoneyWon(double amount) {
        this.moneyWon += Math.max(0.0, amount);
    }

    public double getMoneyLost() {
        return moneyLost;
    }

    public void setMoneyLost(double moneyLost) {
        this.moneyLost = moneyLost;
    }

    public void addMoneyLost(double amount) {
        this.moneyLost += Math.max(0.0, amount);
    }

    public double getNetProfit() {
        return moneyWon - moneyLost;
    }

    public List<ItemStack> getClaimItems() {
        return claimItems;
    }

    public List<ItemStack> getLoserLootItems() {
        return loserLootItems;
    }
}
