package com.ejyqyl.glowcmd.hook;

import com.ejyqyl.glowcmd.GlowCMD;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * Native PlaceholderAPI expansion for GlowCMD.
 * Exposes player utility states, teleportation info, homes, warps, and Essentials drop-in compatible placeholders.
 *
 * @author ejyqyl
 */
public class GlowCMDExpansion extends PlaceholderExpansion {

    private final GlowCMD plugin;
    private final String identifier;

    public GlowCMDExpansion(@NotNull GlowCMD plugin, @NotNull String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;
    }

    public GlowCMDExpansion(@NotNull GlowCMD plugin) {
        this(plugin, "glowcmd");
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return identifier;
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
    public boolean canRegister() {
        return true;
    }

    @Override
    @Nullable
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) {
            return "";
        }

        Player player = offlinePlayer.getPlayer();

        switch (params.toLowerCase(Locale.ROOT)) {
            case "warps_count":
                return String.valueOf(plugin.getWarpManager().getWarps().size());
            case "spawn_set":
                return String.valueOf(plugin.getSpawnManager().getSpawn() != null);
            case "spawn_world":
                return plugin.getSpawnManager().getSpawn() != null ? plugin.getSpawnManager().getSpawn().getWorldName() : "";
            default:
                break;
        }

        if (player == null) {
            return "";
        }

        switch (params.toLowerCase(Locale.ROOT)) {
            case "fly":
            case "is_flying":
                return player.isFlying() ? "yes" : "no";
            case "allow_flight":
                return player.getAllowFlight() ? "yes" : "no";
            case "god":
            case "godmode":
            case "is_god":
                return player.isInvulnerable() ? "yes" : "no";
            case "vanished":
            case "is_vanished":
                return plugin.getVanishManager().isVanished(player) ? "yes" : "no";
            case "speed":
                return String.format(Locale.ROOT, "%.2f", player.isFlying() ? player.getFlySpeed() * 10 : player.getWalkSpeed() * 5);
            case "walk_speed":
            case "walkspeed":
                return String.format(Locale.ROOT, "%.2f", player.getWalkSpeed() * 5);
            case "fly_speed":
            case "flyspeed":
                return String.format(Locale.ROOT, "%.2f", player.getFlySpeed() * 10);
            case "homes_count":
                return String.valueOf(plugin.getHomeManager().getHomes(player.getUniqueId()).size());
            case "homes_max": {
                int max = plugin.getHomeManager().getMaxHomes(player);
                return max >= Integer.MAX_VALUE / 2 ? "∞" : String.valueOf(max);
            }
            case "ping":
                return String.valueOf(player.getPing());
            case "gamemode":
                return player.getGameMode().name().toLowerCase(Locale.ROOT);
            case "pm_recipient": {
                UUID recipientUuid = plugin.getPrivateMessageManager().getReplyTarget(player.getUniqueId());
                if (recipientUuid != null) {
                    Player recipient = Bukkit.getPlayer(recipientUuid);
                    return recipient != null ? recipient.getName() : "";
                }
                return "";
            }
            default:
                return null;
        }
    }
}
