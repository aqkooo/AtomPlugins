package com.ejyqyl.glowcmd.command.teleport;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.manager.TeleportManager;
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

/**
 * /tpdeny [player].
 *
 * @author ejyqyl
 */
public final class TpDenyCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;
    private final TeleportManager teleportManager;

    public TpDenyCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.teleportManager = plugin.getTeleportManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.send(sender, "player-only");
            return true;
        }

        if (!player.hasPermission("glowcmd.tpdeny")) {
            messageManager.send(player, "no-permission");
            return true;
        }

        TeleportManager.TpaRequest request;
        if (args.length > 0) {
            Player specificSender = Bukkit.getPlayer(args[0]);
            if (specificSender == null) {
                messageManager.send(player, "player-not-found", Map.of("{TARGET}", args[0]));
                return true;
            }
            request = teleportManager.getIncomingRequestFrom(player.getUniqueId(), specificSender.getUniqueId());
        } else {
            request = teleportManager.getLatestIncomingRequest(player.getUniqueId());
        }

        if (request == null) {
            messageManager.send(player, "tpa.no-request");
            return true;
        }

        teleportManager.removeRequest(request);

        Player requester = Bukkit.getPlayer(request.senderId());
        if (requester != null && requester.isOnline()) {
            messageManager.send(requester, "tpa.denied-other", Map.of("{PLAYER}", player.getName()));
        }
        messageManager.send(player, "tpa.denied-self");

        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
