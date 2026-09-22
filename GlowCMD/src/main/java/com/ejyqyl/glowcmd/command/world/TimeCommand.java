package com.ejyqyl.glowcmd.command.world;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
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
 * /time, /day, /night.
 *
 * @author ejyqyl
 */
public final class TimeCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;

    public TimeCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("glowcmd.time")) {
            messageManager.send(sender, "no-permission");
            return true;
        }

        World world = (sender instanceof Player p) ? p.getWorld() : Bukkit.getWorlds().get(0);
        long targetTicks;

        String lowerLabel = label.toLowerCase();
        if (lowerLabel.equals("day")) {
            targetTicks = 1000L;
        } else if (lowerLabel.equals("night")) {
            targetTicks = 13000L;
        } else {
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /time <day|night|noon|midnight|ticks>");
                return true;
            }

            String arg = args[0].toLowerCase();
            if (arg.equals("set") && args.length > 1) {
                arg = args[1].toLowerCase();
            }

            targetTicks = switch (arg) {
                case "day" -> 1000L;
                case "noon" -> 6000L;
                case "night" -> 13000L;
                case "midnight" -> 18000L;
                default -> {
                    try {
                        yield Long.parseLong(arg);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cInvalid time value: " + arg);
                        yield -1L;
                    }
                }
            };

            if (targetTicks < 0) {
                return true;
            }
        }

        world.setTime(targetTicks);
        messageManager.send(sender, "time.set", Map.of("{TIME}", String.valueOf(targetTicks), "{WORLD}", world.getName()));
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && alias.equalsIgnoreCase("time")) {
            List<String> list = new ArrayList<>();
            for (String val : List.of("day", "night", "noon", "midnight", "set")) {
                if (val.startsWith(args[0].toLowerCase())) list.add(val);
            }
            return list;
        }
        return Collections.emptyList();
    }
}
