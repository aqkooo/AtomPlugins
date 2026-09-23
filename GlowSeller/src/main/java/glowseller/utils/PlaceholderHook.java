package glowseller.utils;

import glowseller.Main;
import glowseller.models.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderHook extends PlaceholderExpansion {
    private final Main plugin;

    public PlaceholderHook(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "glowseller";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ejyqyl";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        PlayerData data = plugin.getPlayerDataCache().get(player.getUniqueId());
        long points = data != null ? data.getPoints() : 0L;

        return switch (params.toLowerCase()) {
            case "points" -> plugin.getNumberFormatManager().formatNumber(points);
            case "points_raw" -> String.valueOf(points);
            case "booster_active" -> (data != null && data.hasActiveBooster()) ? "true" : "false";
            case "booster_multiplier" -> {
                if (data != null && data.hasActiveBooster()) {
                    double mult = plugin.getBoosterManager().getActiveBoosterMultiplier(player);
                    yield String.format("%.1f", mult);
                }
                yield "1.0";
            }
            case "booster_time_left" -> {
                if (data != null && data.hasActiveBooster()) {
                    yield String.valueOf(data.getBoosterTimeLeftSeconds());
                }
                yield "0";
            }
            case "booster_time_left_formatted" -> {
                if (data != null && data.hasActiveBooster()) {
                    yield plugin.getBoosterManager().formatTime(data.getBoosterTimeLeftSeconds());
                }
                yield "0с";
            }
            case "total_multiplier" -> {
                double total = plugin.getBoosterManager().getTotalMultiplier(player);
                yield String.format("%.2f", total);
            }
            default -> null;
        };
    }
}
