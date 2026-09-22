package com.ejyqyl.glowcmd.command.home;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.manager.HomeManager;
import com.ejyqyl.glowcmd.manager.TeleportManager;
import com.ejyqyl.glowcmd.model.SpawnPoint;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /home [name]. Teleports player to their home.
 *
 * @author ejyqyl
 */
public final class HomeCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final HomeManager homeManager;
    private final TeleportManager teleportManager;

    public HomeCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.homeManager = plugin.getHomeManager();
        this.teleportManager = plugin.getTeleportManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.send(sender, "player-only");
            return true;
        }

        if (!player.hasPermission("glowcmd.home")) {
            messageManager.send(player, "no-permission");
            return true;
        }

        String homeName = args.length > 0 ? args[0].toLowerCase() : "home";
        SpawnPoint home = homeManager.getHome(player.getUniqueId(), homeName);

        if (home == null) {
            // If default home not found and player has exactly 1 home, teleport to that one
            Map<String, SpawnPoint> allHomes = homeManager.getHomes(player.getUniqueId());
            if (args.length == 0 && allHomes.size() == 1) {
                var entry = allHomes.entrySet().iterator().next();
                homeName = entry.getKey();
                home = entry.getValue();
            } else {
                messageManager.send(player, "home.not-found", Map.of("{HOME}", homeName));
                return true;
            }
        }

        Location loc = home.toLocation();
        if (loc == null) {
            messageManager.send(player, "spawn.world-not-found");
            return true;
        }

        teleportManager.setLastLocation(player.getUniqueId(), player.getLocation());
        player.teleport(loc);
        messageManager.send(player, "home.teleport", Map.of("{HOME}", homeName));
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (sender instanceof Player player && args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String name : homeManager.getHomes(player.getUniqueId()).keySet()) {
                if (name.startsWith(args[0].toLowerCase())) list.add(name);
            }
            return list;
        }
        return Collections.emptyList();
    }
}
