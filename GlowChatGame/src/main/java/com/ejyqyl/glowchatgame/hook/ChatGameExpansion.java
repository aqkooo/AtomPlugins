package com.ejyqyl.glowchatgame.hook;

import com.ejyqyl.glowchatgame.GlowChatGame;
import com.ejyqyl.glowchatgame.data.PlayerStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * PlaceholderAPI expansion for GlowChatGame.
 *
 * @author ejyqyl, Glowdevv
 */
public class ChatGameExpansion extends PlaceholderExpansion {

    private final GlowChatGame plugin;
    private final String identifier;

    public ChatGameExpansion(@NotNull GlowChatGame plugin, @NotNull String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;
    }

    public ChatGameExpansion(@NotNull GlowChatGame plugin) {
        this(plugin, "glowchatgame");
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "ejyqyl, Glowdevv";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    @Nullable
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        switch (params.toLowerCase(Locale.ROOT)) {
            case "status":
                return plugin.getGameManager().isGameRunning() ? "active" : "waiting";
            case "last_winner":
                return plugin.getGameManager().getLastWinner();
            case "answer":
                return plugin.getGameManager().getLastAnswer();
            case "time_left":
                if (plugin.getGameManager().getActiveGame() != null) {
                    return String.format(Locale.ROOT, "%.0f", plugin.getGameManager().getActiveGame().getTimeRemainingSeconds());
                }
                return "0";
            default:
                break;
        }

        if (player == null) {
            return "";
        }

        PlayerStats stats = plugin.getStatsManager().getCachedStats(player.getUniqueId());
        if (stats == null) {
            return switch (params.toLowerCase(Locale.ROOT)) {
                case "wins", "games", "rewards" -> "0";
                case "accuracy" -> "0.0";
                default -> null;
            };
        }

        return switch (params.toLowerCase(Locale.ROOT)) {
            case "wins" -> String.valueOf(stats.getWins());
            case "games" -> String.valueOf(stats.getGames());
            case "rewards" -> String.valueOf(stats.getRewardsClaimed());
            case "accuracy" -> stats.getFormattedAccuracy();
            default -> null;
        };
    }
}
