package com.ejyqyl.glowcmd.manager;

import com.ejyqyl.glowcmd.GlowCMD;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player vanish status and visibility across the server.
 *
 * @author ejyqyl
 */
public final class VanishManager {

    private final GlowCMD plugin;
    private final Set<UUID> vanishedPlayers = ConcurrentHashMap.newKeySet();

    public VanishManager(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(@NotNull UUID playerId) {
        return vanishedPlayers.contains(playerId);
    }

    public boolean isVanished(@NotNull Player player) {
        return isVanished(player.getUniqueId());
    }

    public void setVanished(@NotNull Player player, boolean vanish) {
        UUID uuid = player.getUniqueId();
        if (vanish) {
            vanishedPlayers.add(uuid);
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.equals(player) && !other.hasPermission("glowcmd.vanish.see")) {
                    other.hidePlayer(plugin, player);
                }
            }
        } else {
            vanishedPlayers.remove(uuid);
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.equals(player)) {
                    other.showPlayer(plugin, player);
                }
            }
        }
    }

    public void onPlayerJoin(@NotNull Player joiner) {
        boolean canSee = joiner.hasPermission("glowcmd.vanish.see");
        for (UUID uuid : vanishedPlayers) {
            Player vanished = Bukkit.getPlayer(uuid);
            if (vanished != null && !canSee) {
                joiner.hidePlayer(plugin, vanished);
            }
        }
    }

    public void onPlayerQuit(@NotNull Player quiting) {
        vanishedPlayers.remove(quiting.getUniqueId());
    }

    @NotNull
    public Set<UUID> getVanishedPlayers() {
        return Collections.unmodifiableSet(vanishedPlayers);
    }
}
