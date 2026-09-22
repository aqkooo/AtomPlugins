package ru.atomicsqd.glowtrade.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.atomicsqd.glowtrade.config.ConfigManager;
import ru.atomicsqd.glowtrade.manager.TradeManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Обработчик команд /trade и /glowtrade с автодополнением аргументов.
 */
public class TradeCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager configManager;
    private final TradeManager tradeManager;

    public TradeCommand(ConfigManager configManager, TradeManager tradeManager) {
        this.configManager = configManager;
        this.tradeManager = tradeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Команда /glowtrade
        if (command.getName().equalsIgnoreCase("glowtrade")) {
            if (!sender.hasPermission("glowtrade.admin")) {
                configManager.sendMessage(sender, "no-permission");
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                configManager.load();
                configManager.sendMessage(sender, "reload-success");
                return true;
            }

            sender.sendMessage(configManager.getPrefix() + "§e/glowtrade reload §7— перезагрузить конфигурацию плагина.");
            return true;
        }

        // Команда /trade
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cЭта команда доступна только игрокам в игре!");
            return true;
        }

        if (!player.hasPermission("glowtrade.use")) {
            configManager.sendMessage(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            configManager.sendMessage(player, "help");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "accept" -> {
                String targetName = (args.length >= 2) ? args[1] : null;
                tradeManager.acceptRequest(player, targetName);
                return true;
            }
            case "deny" -> {
                String targetName = (args.length >= 2) ? args[1] : null;
                tradeManager.denyRequest(player, targetName);
                return true;
            }
            case "toggle" -> {
                tradeManager.toggleTrades(player);
                return true;
            }
            case "reload" -> {
                if (player.hasPermission("glowtrade.admin")) {
                    configManager.load();
                    configManager.sendMessage(player, "reload-success");
                } else {
                    configManager.sendMessage(player, "no-permission");
                }
                return true;
            }
            default -> {
                // Если передали ник игрока: /trade <ник>
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    configManager.sendMessage(player, "player-offline", "%player%", args[0]);
                    return true;
                }

                tradeManager.sendRequest(player, target);
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("glowtrade")) {
            if (args.length == 1 && sender.hasPermission("glowtrade.admin")) {
                if ("reload".startsWith(args[0].toLowerCase())) {
                    completions.add("reload");
                }
            }
            return completions;
        }

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("accept", "deny", "toggle"));
            if (sender.hasPermission("glowtrade.admin")) {
                subCommands.add("reload");
            }

            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }

            // Добавляем ники онлайн игроков
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equalsIgnoreCase(sender.getName()) && p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
            return completions;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equalsIgnoreCase(sender.getName()) && p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
            return completions;
        }

        return Collections.emptyList();
    }
}
