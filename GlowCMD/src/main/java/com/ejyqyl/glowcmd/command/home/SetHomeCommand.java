package com.ejyqyl.glowcmd.command.home;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.manager.HomeManager;
import com.ejyqyl.glowcmd.model.SpawnPoint;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /sethome [name]. Sets player home location.
 *
 * @author ejyqyl
 */
public final class SetHomeCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final HomeManager homeManager;

    public SetHomeCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.homeManager = plugin.getHomeManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.send(sender, "player-only");
            return true;
        }

        if (!player.hasPermission("glowcmd.sethome")) {
            messageManager.send(player, "no-permission");
            return true;
        }

        String homeName = args.length > 0 ? args[0].toLowerCase() : "home";
        Map<String, SpawnPoint> currentHomes = homeManager.getHomes(player.getUniqueId());

        if (!currentHomes.containsKey(homeName)) {
            int maxHomes = homeManager.getMaxHomes(player);
            if (currentHomes.size() >= maxHomes) {
                messageManager.send(player, "home.limit-reached", Map.of("{MAX}", String.valueOf(maxHomes)));
                return true;
            }
        }

        SpawnPoint point = SpawnPoint.fromLocation(player.getLocation());
        homeManager.setHome(player.getUniqueId(), homeName, point);

        messageManager.send(player, "home.set", Map.of("{HOME}", homeName));
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
