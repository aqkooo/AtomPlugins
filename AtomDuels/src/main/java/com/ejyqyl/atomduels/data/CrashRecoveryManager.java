package com.ejyqyl.atomduels.data;

import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;

/**
 * Recovers interrupted or crashed duels across server restarts.
 * Resolves unresolved escrow balances based on the crash-resolution policy.
 * Authored by ejyqyl.
 */
public class CrashRecoveryManager {

    private final Plugin plugin;
    private final DatabaseManager dbManager;
    private final BetManager betManager;

    public CrashRecoveryManager(Plugin plugin, DatabaseManager dbManager, BetManager betManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.betManager = betManager;
    }

    public void recoverInterruptedDuels() {
        List<DatabaseManager.ActiveDuelRecord> pending = dbManager.getPendingActiveDuels();
        if (pending.isEmpty()) {
            return;
        }

        String resolution = plugin.getConfig().getString("betting.crash-resolution", "DRAW").toUpperCase();
        plugin.getLogger().warning("Found " + pending.size() + " interrupted duel(s) from previous session. Resolving via " + resolution + "...");

        for (DatabaseManager.ActiveDuelRecord record : pending) {
            UUID duelId = record.duelId();
            UUID p1 = record.player1();
            UUID p2 = record.player2();
            double bet = record.betAmount();

            if (bet > 0) {
                switch (resolution) {
                    case "PLAYER_1" -> {
                        if (p1 != null) {
                            betManager.payoutWinner(duelId, p1, p2, bet);
                        } else {
                            betManager.refundBet(duelId, p1, p2, bet, "CRASH_DRAW_FALLBACK");
                        }
                    }
                    case "PLAYER_2" -> {
                        if (p2 != null) {
                            betManager.payoutWinner(duelId, p2, p1, bet);
                        } else {
                            betManager.refundBet(duelId, p1, p2, bet, "CRASH_DRAW_FALLBACK");
                        }
                    }
                    default -> { // DRAW or CANCEL
                        betManager.refundBet(duelId, p1, p2, bet, "CRASH_" + resolution);
                    }
                }
            }

            dbManager.removeActiveDuel(duelId);
        }

        plugin.getLogger().info("Crash recovery completed for all interrupted duels.");
    }
}
