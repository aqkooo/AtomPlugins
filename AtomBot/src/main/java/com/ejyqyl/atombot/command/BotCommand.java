package com.ejyqyl.atombot.command;

import com.ejyqyl.atombot.AtomBot;
import com.ejyqyl.atombot.gui.BotMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Command handler for /bot and /atombot.
 *
 * @author ejyqyl
 */
public class BotCommand implements CommandExecutor, TabCompleter {

    private final AtomBot plugin;
    private static final List<String> SUBS = Arrays.asList("menu", "spawn", "remove", "reload");

    public BotCommand(AtomBot plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                sender.sendMessage("AtomBot configuration reloaded.");
                return true;
            }
            sender.sendMessage("This command is player-only.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            new BotMenu(plugin, player).open();
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "spawn" -> {
                plugin.getBotManager().spawnBot(player);
                plugin.sendMessage(player, "messages.spawned");
            }
            case "remove", "despawn", "delete" -> {
                boolean removed = plugin.getBotManager().removeBot(player.getUniqueId());
                if (removed) {
                    plugin.sendMessage(player, "messages.removed");
                } else {
                    plugin.sendMessage(player, "messages.no-bot");
                }
            }
            case "reload" -> {
                if (!player.hasPermission("atombot.admin")) {
                    plugin.sendMessage(player, "messages.no-permission");
                    return true;
                }
                plugin.reloadConfig();
                plugin.sendMessage(player, "messages.reloaded");
            }
            default -> new BotMenu(plugin, player).open();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String s : SUBS) {
                if (s.startsWith(args[0].toLowerCase())) list.add(s);
            }
            return list;
        }
        return Collections.emptyList();
    }
}
