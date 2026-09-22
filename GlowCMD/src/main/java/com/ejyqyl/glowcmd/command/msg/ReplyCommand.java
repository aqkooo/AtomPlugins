package com.ejyqyl.glowcmd.command.msg;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.manager.PrivateMessageManager;
import org.bukkit.Bukkit;
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
import java.util.UUID;

/**
 * /reply <message> (aliases: /r).
 *
 * @author ejyqyl
 */
public final class ReplyCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final PrivateMessageManager pmManager;

    public ReplyCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.pmManager = plugin.getPrivateMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.send(sender, "player-only");
            return true;
        }

        if (!player.hasPermission("glowcmd.msg")) {
            messageManager.send(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /" + label + " <message>");
            return true;
        }

        UUID targetId = pmManager.getReplyTarget(player.getUniqueId());
        if (targetId == null) {
            messageManager.send(player, "msg.no-reply-target");
            return true;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            messageManager.send(player, "msg.reply-offline");
            return true;
        }

        String content = String.join(" ", args);
        pmManager.setReplyTarget(player.getUniqueId(), target.getUniqueId());

        String senderName = player.getName();
        String targetName = target.getName();

        messageManager.send(player, "msg.format-sender", Map.of("{TARGET}", targetName, "{MESSAGE}", content));
        messageManager.send(target, "msg.format-receiver", Map.of("{SENDER}", senderName, "{MESSAGE}", content));
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
