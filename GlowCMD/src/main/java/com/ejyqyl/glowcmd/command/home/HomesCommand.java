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
 * /homes. Displays list of player's homes.
 *
 * @author ejyqyl
 */
public final class HomesCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final HomeManager homeManager;

    public HomesCommand(@NotNull GlowCMD plugin) {
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

        if (!player.hasPermission("glowcmd.homes")) {
            messageManager.send(player, "no-permission");
            return true;
        }

        Map<String, SpawnPoint> homes = homeManager.getHomes(player.getUniqueId());
        if (homes.isEmpty()) {
            messageManager.send(player, "home.none");
            return true;
        }

        String list = String.join(", ", homes.keySet());
        messageManager.send(player, "home.list", Map.of("{HOMES}", list, "{COUNT}", String.valueOf(homes.size())));
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
