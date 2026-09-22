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
 * /weather, /sun, /rain.
 *
 * @author ejyqyl
 */
public final class WeatherCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;

    public WeatherCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("glowcmd.weather")) {
            messageManager.send(sender, "no-permission");
            return true;
        }

        World world = (sender instanceof Player p) ? p.getWorld() : Bukkit.getWorlds().get(0);
        String lowerLabel = label.toLowerCase();
        String type;

        if (lowerLabel.equals("sun")) {
            type = "clear";
        } else if (lowerLabel.equals("rain")) {
            type = "rain";
        } else {
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /weather <clear|rain|thunder>");
                return true;
            }
            type = args[0].toLowerCase();
        }

        switch (type) {
            case "sun", "clear" -> {
                world.setStorm(false);
                world.setThundering(false);
                world.setClearWeatherDuration(20 * 60 * 30);
            }
            case "rain", "storm" -> {
                world.setStorm(true);
                world.setThundering(false);
            }
            case "thunder" -> {
                world.setStorm(true);
                world.setThundering(true);
            }
            default -> {
                sender.sendMessage("§cInvalid weather type: " + type);
                return true;
            }
        }

        messageManager.send(sender, "weather.set", Map.of("{WEATHER}", type, "{WORLD}", world.getName()));
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && alias.equalsIgnoreCase("weather")) {
            List<String> list = new ArrayList<>();
            for (String val : List.of("clear", "sun", "rain", "thunder")) {
                if (val.startsWith(args[0].toLowerCase())) list.add(val);
            }
            return list;
        }
        return Collections.emptyList();
    }
}
