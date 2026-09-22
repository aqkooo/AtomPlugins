package ru.atomicsqd.atomreactor.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.atomicsqd.atomreactor.AtomReactor;
import ru.atomicsqd.atomreactor.config.ReactorLevel;
import ru.atomicsqd.atomreactor.model.Reactor;
import ru.atomicsqd.atomreactor.service.HologramService;
import ru.atomicsqd.atomreactor.service.ReactorManager;
import ru.atomicsqd.atomreactor.service.ReactorTicker;
import ru.atomicsqd.atomreactor.util.ColorUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command handler for /atomreactor (/areactor, /reactor).
 */
public class AtomReactorCommand implements CommandExecutor, TabCompleter {

    private final AtomReactor plugin;
    private final ReactorManager reactorManager;
    private final HologramService hologramService;
    private final ReactorTicker reactorTicker;

    public AtomReactorCommand(AtomReactor plugin, ReactorManager reactorManager,
                              HologramService hologramService, ReactorTicker reactorTicker) {
        this.plugin = plugin;
        this.reactorManager = reactorManager;
        this.hologramService = hologramService;
        this.reactorTicker = reactorTicker;
    }

    private String getPrefix() {
        return plugin.getConfigManager().getPrefix();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("atomreactor.admin")) {
            sender.sendMessage(ColorUtil.component(getPrefix() + plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.component(getPrefix() + "&cИспользование: &e/areactor give <игрок> [уровень]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtil.component(getPrefix() + "&cИгрок '" + args[1] + "' не найден!"));
            return;
        }

        int level = 1;
        if (args.length >= 3) {
            try {
                level = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException e) {
                sender.sendMessage(ColorUtil.component(getPrefix() + "&cНеверный уровень: " + args[2]));
                return;
            }
        }

        ItemStack item = reactorManager.createReactorItem(level);
        target.getInventory().addItem(item);

        String msg = plugin.getConfigManager().getMessage("reactor-given")
                .replace("%player%", target.getName())
                .replace("%level%", String.valueOf(level));
        sender.sendMessage(ColorUtil.component(getPrefix() + msg));
    }

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().load();
        plugin.getEconomyService().init();
        reactorManager.loadReactors();
        reactorTicker.start();

        for (Reactor r : reactorManager.getAllReactors()) {
            hologramService.updateHologram(r);
        }

        sender.sendMessage(ColorUtil.component(getPrefix() + plugin.getConfigManager().getMessage("reload-success")));
    }

    private void handleList(CommandSender sender) {
        var all = reactorManager.getAllReactors();
        if (all.isEmpty()) {
            sender.sendMessage(ColorUtil.component(getPrefix() + "&7Список реакторов пуст."));
            return;
        }

        sender.sendMessage(ColorUtil.component("<gradient:#FF5F6D:#FFC371>&lСписок активных Реакторов (" + all.size() + "):</gradient>"));
        for (Reactor r : all) {
            ReactorLevel lvl = plugin.getConfigManager().getLevel(r.getLevel());
            sender.sendMessage(ColorUtil.component(" &8• <gradient:#56CCF2:#2F80ED>" + r.getOwnerName() + "</gradient> &8| &fУр. "
                    + r.getLevel() + " &8| &7" + r.getWorldName() + " (" + r.getX() + ", " + r.getY() + ", " + r.getZ() + ")"
                    + " &8| " + (r.isActive() ? "&aВКЛ" : "&cВЫКЛ")));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.component("<gradient:#FF5F6D:#FFC371>&m               &r <gradient:#FF5F6D:#FFC371>&lAtomReactor Справка</gradient> <gradient:#FF5F6D:#FFC371>&m               </gradient>"));
        sender.sendMessage(ColorUtil.component("&e/areactor give <игрок> [ур] &8— &7Выдать блок реактора"));
        sender.sendMessage(ColorUtil.component("&e/areactor list &8— &7Список установленных реакторов"));
        sender.sendMessage(ColorUtil.component("&e/areactor reload &8— &7Перезагрузить конфигурацию"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("atomreactor.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subs = Arrays.asList("give", "list", "reload");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            return List.of("1", "2", "3", "4", "5");
        }

        return Collections.emptyList();
    }
}
