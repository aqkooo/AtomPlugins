package com.ejyqyl.atompvpbot.hook;

import com.ejyqyl.atompvpbot.data.PlayerData;
import com.ejyqyl.atompvpbot.data.StatsManager;
import com.ejyqyl.atompvpbot.fight.FightManager;
import com.ejyqyl.atompvpbot.gui.PlayerFightPreferences;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for AtomPvPbot (%atompvpbot_*%).
 *
 * @author ejyqyl
 */
public class BotPlaceholderExpansion extends PlaceholderExpansion {

    private final Plugin plugin;
    private final StatsManager statsManager;
    private final FightManager fightManager;

    public BotPlaceholderExpansion(Plugin plugin, StatsManager statsManager, FightManager fightManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.fightManager = fightManager;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "atompvpbot";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "ejyqyl";
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
    @Nullable
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || offlinePlayer.getUniqueId() == null) {
            return "";
        }

        PlayerData data = statsManager.getPlayerData(offlinePlayer.getUniqueId(), offlinePlayer.getName());

        return switch (params.toLowerCase()) {
            case "wins" -> String.valueOf(data.getWins());
            case "losses" -> String.valueOf(data.getLosses());
            case "winrate" -> String.format("%.1f", data.getWinRate());
            case "winstreak" -> String.valueOf(data.getWinStreak());
            case "beststreak" -> String.valueOf(data.getBestStreak());
            case "totalgames" -> String.valueOf(data.getTotalGames());
            case "in_fight" -> {
                if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
                    yield String.valueOf(fightManager.isInFight(offlinePlayer.getPlayer()));
                }
                yield "false";
            }
            case "difficulty" -> {
                PlayerFightPreferences prefs = PlayerFightPreferences.get(offlinePlayer.getUniqueId());
                yield prefs.getDifficulty().name();
            }
            case "behavior" -> {
                PlayerFightPreferences prefs = PlayerFightPreferences.get(offlinePlayer.getUniqueId());
                yield prefs.getBehaviorMode().name();
            }
            case "kit" -> {
                PlayerFightPreferences prefs = PlayerFightPreferences.get(offlinePlayer.getUniqueId());
                yield prefs.getKitName();
            }
            default -> null;
        };
    }
}
