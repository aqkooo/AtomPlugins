package com.ejyqyl.atomduels.hook;

import com.ejyqyl.atomduels.AtomDuels;
import com.ejyqyl.atomduels.data.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion providing %atomduels% placeholders.
 * Authored by ejyqyl (https://github.com/aqkooo).
 */
public class DuelsExpansion extends PlaceholderExpansion {

    private final AtomDuels plugin;

    public DuelsExpansion(AtomDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "atomduels";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ejyqyl";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        PlayerData data = plugin.getStatsManager().getPlayerData(player.getUniqueId(), player.getName());
        if (data == null) return "";

        return switch (params.toLowerCase()) {
            case "elo" -> String.valueOf(data.getElo());
            case "rank" -> data.getRank().getDisplayName();
            case "rank_symbol" -> data.getRank().getSymbol();
            case "rank_formatted" -> data.getRank().getFormattedName();
            case "wins" -> String.valueOf(data.getWins());
            case "losses" -> String.valueOf(data.getLosses());
            case "draws" -> String.valueOf(data.getDraws());
            case "total" -> String.valueOf(data.getTotalMatches());
            case "winrate" -> String.format("%.1f", data.getWinRate());
            case "streak" -> String.valueOf(data.getWinStreak());
            case "best_streak" -> String.valueOf(data.getBestStreak());
            case "money_won" -> String.format("%.2f", data.getMoneyWon());
            case "money_lost" -> String.format("%.2f", data.getMoneyLost());
            case "profit" -> String.format("%.2f", data.getNetProfit());
            case "calibration" -> String.valueOf(Math.min(10, data.getCalibrationMatches()));
            case "in_duel" -> String.valueOf(plugin.getDuelManager().isInDuel(player.getUniqueId()));
            case "in_queue" -> String.valueOf(plugin.getQueueManager().isInQueue(player.getUniqueId()));
            default -> null;
        };
    }
}
