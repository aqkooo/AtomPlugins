package com.ejyqyl.glowcmd.command.admin;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /sudo <player> <c:message|command>.
 *
 * @author ejyqyl
 */
public final class SudoCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;

    public SudoCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("glowcmd.sudo")) {
            messageManager.send(sender, "no-permission");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /sudo <player> <c:chat|command>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            messageManager.send(sender, "player-not-found", Map.of("{TARGET}", args[0]));
            return true;
        }

        String toRun = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (toRun.startsWith("c:") || toRun.startsWith("C:")) {
            String chat = toRun.substring(2).trim();
            target.chat(chat);
        } else {
            if (toRun.startsWith("/")) {
                toRun = toRun.substring(1);
            }
            target.performCommand(toRun);
        }

        sender.sendMessage("§aForced §e" + target.getName() + " §ato: §f" + toRun);
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("glowcmd.sudo")) {
            return null;
        }
        return Collections.emptyList();
    }
}
