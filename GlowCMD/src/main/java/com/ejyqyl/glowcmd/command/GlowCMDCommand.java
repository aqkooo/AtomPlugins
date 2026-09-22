package com.ejyqyl.glowcmd.command;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Main command handler for /glowcmd (and alias /gcmd).
 * Handles configuration reloading and administration tasks.
 *
 * @author ejyqyl
 */
public final class GlowCMDCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;

    public GlowCMDCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender.hasPermission("glowcmd.reload")) {
                sender.sendMessage("§bGlowCMD §7v" + plugin.getDescription().getVersion() + " by §fejyqyl");
                sender.sendMessage("§7Usage: /" + label + " reload");
            } else {
                messageManager.send(sender, "no-permission");
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("reload")) {
            if (!sender.hasPermission("glowcmd.reload")) {
                messageManager.send(sender, "no-permission");
                return true;
            }

            try {
                plugin.reloadAll();
                messageManager.send(sender, "reload.success");
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "An error occurred while reloading GlowCMD", ex);
                messageManager.send(sender, "reload.error");
            }
            return true;
        }

        messageManager.send(sender, "unknown-command");
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            if (sender.hasPermission("glowcmd.reload")) {
                String input = args[0].toLowerCase();
                if ("reload".startsWith(input)) {
                    return Collections.singletonList("reload");
                }
            }
        }
        return Collections.emptyList();
    }
}
