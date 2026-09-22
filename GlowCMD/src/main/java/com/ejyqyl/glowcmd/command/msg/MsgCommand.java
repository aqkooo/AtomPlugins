package com.ejyqyl.glowcmd.command.msg;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.manager.PrivateMessageManager;
import com.ejyqyl.glowcmd.util.ColorUtil;
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
 * /msg <player> <message> (aliases: /tell, /w, /m).
 *
 * @author ejyqyl
 */
public final class MsgCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final PrivateMessageManager pmManager;

    public MsgCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.pmManager = plugin.getPrivateMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("glowcmd.msg")) {
            messageManager.send(sender, "no-permission");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /" + label + " <player> <message>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            messageManager.send(sender, "player-not-found", Map.of("{TARGET}", args[0]));
            return true;
        }

        if (sender.equals(target)) {
            sender.sendMessage("§cYou cannot message yourself.");
            return true;
        }

        String content = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (sender instanceof Player p) {
            pmManager.setReplyTarget(p.getUniqueId(), target.getUniqueId());
        }

        String senderName = sender.getName();
        String targetName = target.getName();

        messageManager.send(sender, "msg.format-sender", Map.of("{TARGET}", targetName, "{MESSAGE}", content));
        messageManager.send(target, "msg.format-receiver", Map.of("{SENDER}", senderName, "{MESSAGE}", content));
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return null;
        }
        return Collections.emptyList();
    }
}
