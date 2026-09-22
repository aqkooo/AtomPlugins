package com.ejyqyl.glowcmd.command.home;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.manager.HomeManager;
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
 * /delhome [name].
 *
 * @author ejyqyl
 */
public final class DelHomeCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final HomeManager homeManager;

    public DelHomeCommand(@NotNull GlowCMD plugin) {
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

        if (!player.hasPermission("glowcmd.delhome")) {
            messageManager.send(player, "no-permission");
            return true;
        }

        String homeName = args.length > 0 ? args[0].toLowerCase() : "home";
        if (homeManager.deleteHome(player.getUniqueId(), homeName)) {
            messageManager.send(player, "home.deleted", Map.of("{HOME}", homeName));
        } else {
            messageManager.send(player, "home.not-found", Map.of("{HOME}", homeName));
        }
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
