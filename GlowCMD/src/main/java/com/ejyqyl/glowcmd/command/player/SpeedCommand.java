package com.ejyqyl.glowcmd.command.player;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import org.bukkit.Bukkit;
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
 * /speed <walk|fly> <1-10> [player] or /speed <1-10> [player].
 *
 * @author ejyqyl
 */
public final class SpeedCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;

    public SpeedCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("glowcmd.speed")) {
            messageManager.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /speed [fly|walk] <1-10> [player]");
            return true;
        }

        boolean isFly = false;
        int speedIndex = 0;
        Player target;

        if (args[0].equalsIgnoreCase("fly")) {
            isFly = true;
            speedIndex = 1;
        } else if (args[0].equalsIgnoreCase("walk")) {
            isFly = false;
            speedIndex = 1;
        } else {
            if (sender instanceof Player p) {
                isFly = p.isFlying();
            }
        }

        if (args.length <= speedIndex) {
            sender.sendMessage("§cUsage: /speed [fly|walk] <1-10> [player]");
            return true;
        }

        float speedValue;
        try {
            float val = Float.parseFloat(args[speedIndex]);
            if (val < 1 || val > 10) {
                messageManager.send(sender, "speed.invalid");
                return true;
            }
            speedValue = val / 10.0f;
        } catch (NumberFormatException e) {
            messageManager.send(sender, "speed.invalid");
            return true;
        }

        if (args.length > speedIndex + 1) {
            if (!sender.hasPermission("glowcmd.speed.others")) {
                messageManager.send(sender, "no-permission");
                return true;
            }
            target = Bukkit.getPlayer(args[speedIndex + 1]);
            if (target == null) {
                messageManager.send(sender, "player-not-found", Map.of("{TARGET}", args[speedIndex + 1]));
                return true;
            }
        } else {
            if (!(sender instanceof Player p)) {
                messageManager.send(sender, "player-only");
                return true;
            }
            target = p;
        }

        if (isFly) {
            target.setFlySpeed(speedValue);
            messageManager.send(sender, "speed.set-fly", Map.of("{SPEED}", String.valueOf(args[speedIndex]), "{TARGET}", target.getName()));
        } else {
            target.setWalkSpeed(speedValue);
            messageManager.send(sender, "speed.set-walk", Map.of("{SPEED}", String.valueOf(args[speedIndex]), "{TARGET}", target.getName()));
        }

        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String opt : List.of("fly", "walk", "1", "2", "3", "5", "10")) {
                if (opt.startsWith(args[0].toLowerCase())) list.add(opt);
            }
            return list;
        }
        return Collections.emptyList();
    }
}
