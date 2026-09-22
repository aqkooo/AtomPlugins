package ru.atomicsqd.atommessage.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.atomicsqd.atommessage.AtomMessage;
import ru.atomicsqd.atommessage.config.ConfigManager;
import ru.atomicsqd.atommessage.manager.ChatAnnouncementManager;
import ru.atomicsqd.atommessage.manager.TabMessageManager;
import ru.atomicsqd.atommessage.model.ChatAnnouncement;
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
                    int tabCount = cfg.getTabMessages().size();
                    int chatCount = cfg.getChatAnnouncements().size();
                    sender.sendMessage(cfg.getMessageComponent("reload-success",
                            "{tab_count}", String.valueOf(tabCount),
                            "{chat_count}", String.valueOf(chatCount)));
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to reload AtomMessage", ex);
                    sender.sendMessage(cfg.getMessageComponent("reload-error"));
                }
            }
            case "broadcast", "bc" -> {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.parseComponent("&cИспользование: /" + label + " broadcast <id>", null));
                    return true;
                }
                String id = args[1];
                ChatAnnouncementManager chatManager = plugin.getChatAnnouncementManager();
                boolean success = chatManager.broadcastById(id, sender);
                if (success) {
                    sender.sendMessage(cfg.getMessageComponent("broadcast-success", "{id}", id));
                } else {
                    sender.sendMessage(cfg.getMessageComponent("broadcast-not-found", "{id}", id));
                }
            }
            case "test" -> {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.parseComponent("&cИспользование: /" + label + " test <текст/теги...>", null));
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) sb.append(" ");
                    sb.append(args[i]);
                }
                String raw = sb.toString();
                Player targetPlayer = sender instanceof Player p ? p : null;

                sender.sendMessage(ColorUtil.parseComponent("<gradient:#00B5FD:#7670E5><b>[AtomMessage Test]</b></gradient> &7Предпросмотр форматирования:", targetPlayer));
                Component parsed = ColorUtil.parseComponent(raw, targetPlayer);
                sender.sendMessage(parsed);
            }
            case "list" -> {
                sender.sendMessage(ColorUtil.parseComponent("<gradient:#00B5FD:#7670E5><b>=== Список сообщений AtomMessage ===</b></gradient>", null));

                // TAB list
                List<TabMessage> tabs = cfg.getTabMessages();
                sender.sendMessage(ColorUtil.parseComponent("&eСообщения в ТАБ (&f" + tabs.size() + "&e):", null));
                for (TabMessage tm : tabs) {
                    sender.sendMessage(ColorUtil.parseComponent(" &7- &f" + tm.getId() + " &8(&7" + tm.getDurationSeconds() + "s&8)", null));
                }

                // Chat list
                List<ChatAnnouncement> chats = cfg.getChatAnnouncements();
                sender.sendMessage(ColorUtil.parseComponent("&eОбъявления в ЧАТ (&f" + chats.size() + "&e):", null));
                for (ChatAnnouncement ca : chats) {
                    sender.sendMessage(ColorUtil.parseComponent(" &7- &a" + ca.getId() + " &8(&7" + (ca.getIntervalSeconds() > 0 ? ca.getIntervalSeconds() : cfg.getChatDefaultInterval()) + "s&8)", null));
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
                String id = current != null ? current.getId() : "unknown";
                sender.sendMessage(cfg.getMessageComponent("next-success", "{id}", id));
            }
            case "info" -> {
                TabMessageManager manager = plugin.getTabMessageManager();
                ChatAnnouncementManager chatManager = plugin.getChatAnnouncementManager();
                TabMessage current = manager.getCurrentMessage();
                String tabId = current != null ? current.getId() : "none";
                String chatId = chatManager.getLastBroadcastId();
                int nextSec = chatManager.getSecondsUntilNextBroadcast();

                sender.sendMessage(cfg.getMessageComponent("info-format",
                        "{tab_current}", tabId,
                        "{tab_total}", String.valueOf(manager.getTotalCount()),
                        "{chat_current}", chatId,
                        "{chat_total}", String.valueOf(chatManager.getTotalAnnouncements()),
                        "{chat_next}", String.valueOf(nextSec)
                ));
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
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("atommessage.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subs = List.of("broadcast", "test", "list", "reload", "next", "info");
            String prefix = args[0].toLowerCase();
            return subs.stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("broadcast") || args[0].equalsIgnoreCase("bc"))) {
            String prefix = args[1].toLowerCase();
            return plugin.getConfigManager().getChatAnnouncements().stream()
                    .map(ChatAnnouncement::getId)
                    .filter(id -> id.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
