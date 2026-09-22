package ru.atomicsqd.atommessage.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.atomicsqd.atommessage.AtomMessage;
import ru.atomicsqd.atommessage.config.ConfigManager;
import ru.atomicsqd.atommessage.manager.TabMessageManager;
import ru.atomicsqd.atommessage.model.TabMessage;
import ru.atomicsqd.atommessage.util.ColorUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class AtomMessageCommand implements CommandExecutor, TabCompleter {
    private final AtomMessage plugin;

    public AtomMessageCommand(@NotNull AtomMessage plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        ConfigManager cfg = plugin.getConfigManager();

        if (!sender.hasPermission("atommessage.admin")) {
            sender.sendMessage(cfg.getMessageComponent("no-permission"));
            return true;
        }

        if (args.length == 0) {
            for (Component line : cfg.getHelpList()) {
                sender.sendMessage(line);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> {
                try {
                    plugin.reload();
                    int count = plugin.getConfigManager().getTabMessages().size();
                    sender.sendMessage(cfg.getMessageComponent("reload-success", "{count}", String.valueOf(count)));
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to reload AtomMessage", ex);
                    sender.sendMessage(cfg.getMessageComponent("reload-error"));
                }
            }
            case "next" -> {
                TabMessageManager manager = plugin.getTabMessageManager();
                if (manager.getTotalCount() == 0) {
                    sender.sendMessage(cfg.getMessageComponent("no-messages"));
                    return true;
                }
                manager.next();
                TabMessage current = manager.getCurrentMessage();
                String id = current != null ? current.getId() : "-";
                sender.sendMessage(cfg.getMessageComponent("next-message", "{id}", id));
            }
            case "prev" -> {
                TabMessageManager manager = plugin.getTabMessageManager();
                if (manager.getTotalCount() == 0) {
                    sender.sendMessage(cfg.getMessageComponent("no-messages"));
                    return true;
                }
                manager.prev();
                TabMessage current = manager.getCurrentMessage();
                String id = current != null ? current.getId() : "-";
                sender.sendMessage(cfg.getMessageComponent("prev-message", "{id}", id));
            }
            case "list" -> {
                List<TabMessage> list = plugin.getConfigManager().getTabMessages();
                if (list.isEmpty()) {
                    sender.sendMessage(cfg.getMessageComponent("no-messages"));
                    return true;
                }
                sender.sendMessage(cfg.getMessageComponent("list-header", "{count}", String.valueOf(list.size())));
                for (TabMessage msg : list) {
                    String preview = msg.getLines().isEmpty() ? "" : msg.getLines().get(0);
                    Component comp = cfg.getMessageComponent("list-format",
                            "{id}", msg.getId(),
                            "{duration}", String.valueOf(msg.getDurationSeconds()),
                            "{preview}", preview);
                    sender.sendMessage(comp);
                }
            }
            case "status" -> {
                TabMessageManager manager = plugin.getTabMessageManager();
                TabMessage current = manager.getCurrentMessage();
                if (current == null) {
                    sender.sendMessage(cfg.getMessageComponent("no-messages"));
                    return true;
                }
                sender.sendMessage(cfg.getMessageComponent("current-status",
                        "{id}", current.getId(),
                        "{left}", String.valueOf(manager.getSecondsRemaining())));
            }
            default -> {
                for (Component line : cfg.getHelpList()) {
                    sender.sendMessage(line);
                }
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("atommessage.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = List.of("reload", "next", "prev", "list", "status");
            String search = args[0].toLowerCase();
            return completions.stream().filter(s -> s.startsWith(search)).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
