package com.ejyqyl.atomduels.data;

import com.ejyqyl.atomduels.hook.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * Escrow betting manager.
 * Holds stakes in escrow, burns 5% fee on victory, records transactions,
 * and retries failed transactions up to 20 times.
 * Authored by ejyqyl.
 */
public class BetManager {

    private final Plugin plugin;
    private final VaultHook vaultHook;
    private final DatabaseManager dbManager;
    private final StatsManager statsManager;
    private double feePercent = 5.0;

    private static final int MAX_RETRIES = 20;
    private final ConcurrentLinkedQueue<RetryTransaction> retryQueue = new ConcurrentLinkedQueue<>();

    private record RetryTransaction(UUID duelId, UUID player, double amount, String type, int attempt) {}

    public BetManager(Plugin plugin, VaultHook vaultHook, DatabaseManager dbManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.vaultHook = vaultHook;
        this.dbManager = dbManager;
        this.statsManager = statsManager;
        this.feePercent = plugin.getConfig().getDouble("betting.fee-percent", 5.0);

        startRetryWorker();
    }

    public void setFeePercent(double feePercent) {
        this.feePercent = feePercent;
    }

    public double getFeePercent() {
        return feePercent;
    }

    /**
     * Attempts to freeze bets from both participants in escrow.
     * Returns true if both participants had sufficient funds and escrow was locked.
     */
    public boolean holdBet(UUID duelId, UUID player1, UUID player2, double betAmount) {
        if (betAmount <= 0) {
            return true;
        }
        if (!vaultHook.hasEconomy()) {
            return true;
        }

        boolean p1IsPlayer = player1 != null;
        boolean p2IsPlayer = player2 != null;

        if (p1IsPlayer && !vaultHook.hasEnough(player1, betAmount)) {
            return false;
        }
        if (p2IsPlayer && !vaultHook.hasEnough(player2, betAmount)) {
            return false;
        }

        // Withdraw from player 1
        if (p1IsPlayer) {
            boolean success = vaultHook.withdraw(player1, betAmount);
            if (!success) {
                return false;
            }
            dbManager.recordTransaction(duelId, player1, betAmount, "ESCROW_HOLD", "SUCCESS", 1);
        }

        // Withdraw from player 2
        if (p2IsPlayer) {
            boolean success = vaultHook.withdraw(player2, betAmount);
            if (!success) {
                // Refund player 1 immediately
                if (p1IsPlayer) {
                    vaultHook.deposit(player1, betAmount);
                    dbManager.recordTransaction(duelId, player1, betAmount, "ESCROW_ROLLBACK", "SUCCESS", 1);
                }
                return false;
            }
            dbManager.recordTransaction(duelId, player2, betAmount, "ESCROW_HOLD", "SUCCESS", 1);
        }

        return true;
    }

    /**
     * Refunds both participants in escrow (e.g. on draw, cancellation, or crash).
     */
    public void refundBet(UUID duelId, UUID player1, UUID player2, double betAmount, String reason) {
        if (betAmount <= 0) return;

        if (player1 != null) {
            depositWithRetry(duelId, player1, betAmount, "ESCROW_REFUND");
        }
        if (player2 != null) {
            depositWithRetry(duelId, player2, betAmount, "ESCROW_REFUND");
        }
    }

    /**
     * Pays out the winning player, taking the configured fee, and registers stats.
     */
    public void payoutWinner(UUID duelId, UUID winner, UUID loser, double betAmount) {
        if (betAmount <= 0) return;

        double totalPot = betAmount * 2.0;
        double fee = totalPot * (feePercent / 100.0);
        double winnerPrize = totalPot - fee;

        if (winner != null) {
            depositWithRetry(duelId, winner, winnerPrize, "BET_WIN");
            PlayerData winnerData = statsManager.getPlayerData(winner, null);
            if (winnerData != null) {
                winnerData.addMoneyWon(winnerPrize - betAmount);
                statsManager.savePlayerData(winner);
            }
        }

        if (loser != null) {
            PlayerData loserData = statsManager.getPlayerData(loser, null);
            if (loserData != null) {
                loserData.addMoneyLost(betAmount);
                statsManager.savePlayerData(loser);
            }
        }
    }

    private void depositWithRetry(UUID duelId, UUID player, double amount, String type) {
        if (!vaultHook.hasEconomy()) return;

        boolean success = vaultHook.deposit(player, amount);
        if (success) {
            dbManager.recordTransaction(duelId, player, amount, type, "SUCCESS", 1);
        } else {
            plugin.getLogger().warning("Failed to deposit " + amount + " to " + player + " (" + type + "). Queuing for retry.");
            retryQueue.add(new RetryTransaction(duelId, player, amount, type, 1));
        }
    }

    private void startRetryWorker() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            RetryTransaction tx;
            while ((tx = retryQueue.poll()) != null) {
                boolean success = vaultHook.deposit(tx.player(), tx.amount());
                if (success) {
                    plugin.getLogger().info("Successfully processed queued transaction for " + tx.player() + " after attempt " + tx.attempt());
                    dbManager.recordTransaction(tx.duelId(), tx.player(), tx.amount(), tx.type(), "RETRY_SUCCESS", tx.attempt());
                } else if (tx.attempt() < MAX_RETRIES) {
                    retryQueue.add(new RetryTransaction(tx.duelId(), tx.player(), tx.amount(), tx.type(), tx.attempt() + 1));
                } else {
                    plugin.getLogger().log(Level.SEVERE, "CRITICAL: Transaction failed after 20 attempts for player " + tx.player() + " amount " + tx.amount());
                    dbManager.recordTransaction(tx.duelId(), tx.player(), tx.amount(), tx.type(), "FAILED_EXHAUSTED", tx.attempt());
                }
            }
        }, 100L, 100L); // check every 5 seconds
    }
}
