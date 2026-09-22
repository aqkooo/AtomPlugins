package ru.atomicsqd.atomregen.command;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.atomicsqd.atomregen.AtomRegen;
import ru.atomicsqd.atomregen.model.BlockVector;
import ru.atomicsqd.atomregen.model.CuboidRegion;
import ru.atomicsqd.atomregen.service.RegenEngine;
import ru.atomicsqd.atomregen.service.RegionManager;
import ru.atomicsqd.atomregen.service.SelectionService;
import ru.atomicsqd.atomregen.util.ColorUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main command handler for /atomregen (/aregen, /atomreg, /areg).
 */
public class AtomRegenCommand implements CommandExecutor, TabCompleter {

    private final AtomRegen plugin;
    private final SelectionService selectionService;
    private final RegionManager regionManager;
    private final RegenEngine regenEngine;

    public AtomRegenCommand(AtomRegen plugin, SelectionService selectionService,
                            RegionManager regionManager, RegenEngine regenEngine) {
        this.plugin = plugin;
        this.selectionService = selectionService;
        this.regionManager = regionManager;
        this.regenEngine = regenEngine;
    }

    private String getPrefix() {
        return plugin.getConfig().getString("prefix", "");
    }

    private String getMsg(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("atomregen.admin")) {
            sender.sendMessage(ColorUtil.color(getPrefix() + getMsg("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "wand" -> handleWand(sender);
            case "pos1" -> handlePos1(sender);
            case "pos2" -> handlePos2(sender);
            case "create" -> handleCreate(sender, args);
            case "delete", "remove" -> handleDelete(sender, args);
            case "setdelay", "delay" -> handleSetDelay(sender, args);
            case "reset", "restore" -> handleReset(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.color(getPrefix() + getMsg("only-players")));
            return;
        }

        ItemStack wand = selectionService.createWand();
        player.getInventory().addItem(wand);
        player.sendMessage(ColorUtil.color(getPrefix() + getMsg("wand-given")));
    }

    private void handlePos1(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.color(getPrefix() + getMsg("only-players")));
            return;
        }

        Location loc = player.getLocation().getBlock().getLocation();
        selectionService.setPos1(player, loc);
        String msg = getMsg("pos1-set")
                .replace("%x%", String.valueOf(loc.getBlockX()))
                .replace("%y%", String.valueOf(loc.getBlockY()))
                .replace("%z%", String.valueOf(loc.getBlockZ()))
                .replace("%world%", loc.getWorld().getName());
        player.sendMessage(ColorUtil.color(getPrefix() + msg));
    }

    private void handlePos2(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.color(getPrefix() + getMsg("only-players")));
            return;
        }

        Location loc = player.getLocation().getBlock().getLocation();
        selectionService.setPos2(player, loc);
        String msg = getMsg("pos2-set")
                .replace("%x%", String.valueOf(loc.getBlockX()))
                .replace("%y%", String.valueOf(loc.getBlockY()))
                .replace("%z%", String.valueOf(loc.getBlockZ()))
                .replace("%world%", loc.getWorld().getName());
        player.sendMessage(ColorUtil.color(getPrefix() + msg));
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.color(getPrefix() + getMsg("only-players")));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ColorUtil.color(getPrefix() + "&cИспользование: &e/ar create <имя> [время_регенерации_сек]"));
            return;
        }

        String name = args[1];
        if (regionManager.getRegion(name) != null) {
            String msg = getMsg("region-already-exists").replace("%region%", name);
            sender.sendMessage(ColorUtil.color(getPrefix() + msg));
            return;
        }

        if (!selectionService.hasBothPositions(player.getUniqueId())) {
            sender.sendMessage(ColorUtil.color(getPrefix() + getMsg("positions-not-set")));
            return;
        }

        Location p1 = selectionService.getPos1(player.getUniqueId());
        Location p2 = selectionService.getPos2(player.getUniqueId());

        if (!p1.getWorld().equals(p2.getWorld())) {
            sender.sendMessage(ColorUtil.color(getPrefix() + getMsg("different-worlds")));
            return;
        }

        long delay = plugin.getConfig().getLong("default-regen-delay-seconds", 15);
        if (args.length >= 3) {
            try {
                delay = Math.max(1, Long.parseLong(args[2]));
            } catch (NumberFormatException e) {
                sender.sendMessage(ColorUtil.color(getPrefix() + "&cНеверный формат времени: " + args[2]));
                return;
            }
        }

        CuboidRegion region = regionManager.createRegion(
                name,
                p1.getWorld(),
                BlockVector.fromLocation(p1),
                BlockVector.fromLocation(p2),
                delay
        );

        String msg = getMsg("region-created")
                .replace("%region%", region.getName())
                .replace("%blocks%", String.valueOf(region.getVolume()))
                .replace("%delay%", String.valueOf(region.getRegenDelaySeconds()));
        sender.sendMessage(ColorUtil.color(getPrefix() + msg));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.color(getPrefix() + "&cИспользование: &e/ar delete <имя>"));
            return;
        }

        String name = args[1];
        CuboidRegion region = regionManager.getRegion(name);
        if (region == null) {
            String msg = getMsg("region-not-found").replace("%region%", name);
            sender.sendMessage(ColorUtil.color(getPrefix() + msg));
            return;
        }

        regenEngine.clearPendingForRegion(region);
        regionManager.deleteRegion(name);
        String msg = getMsg("region-deleted").replace("%region%", name);
        sender.sendMessage(ColorUtil.color(getPrefix() + msg));
    }

    private void handleSetDelay(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.color(getPrefix() + "&cИспользование: &e/ar setdelay <имя> <секунды>"));
            return;
        }

        String name = args[1];
        CuboidRegion region = regionManager.getRegion(name);
        if (region == null) {
            String msg = getMsg("region-not-found").replace("%region%", name);
            sender.sendMessage(ColorUtil.color(getPrefix() + msg));
            return;
        }

        try {
            long newDelay = Math.max(1, Long.parseLong(args[2]));
            region.setRegenDelaySeconds(newDelay);
            regionManager.saveRegion(region);

            String msg = getMsg("delay-updated")
                    .replace("%region%", region.getName())
                    .replace("%delay%", String.valueOf(newDelay));
            sender.sendMessage(ColorUtil.color(getPrefix() + msg));
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtil.color(getPrefix() + "&cНеверное число секунд: " + args[2]));
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.color(getPrefix() + "&cИспользование: &e/ar reset <имя>"));
            return;
        }

        String name = args[1];
        CuboidRegion region = regionManager.getRegion(name);
        if (region == null) {
            String msg = getMsg("region-not-found").replace("%region%", name);
            sender.sendMessage(ColorUtil.color(getPrefix() + msg));
            return;
        }

        regenEngine.clearPendingForRegion(region);
        int restored = regionManager.resetRegion(region);

        String msg = getMsg("region-reset")
                .replace("%region%", region.getName())
                .replace("%blocks%", String.valueOf(restored));
        sender.sendMessage(ColorUtil.color(getPrefix() + msg));
    }

    private void handleList(CommandSender sender) {
        var all = regionManager.getAllRegions();
        if (all.isEmpty()) {
            sender.sendMessage(ColorUtil.color(getPrefix() + "&7Список регионов пуст."));
            return;
        }

        sender.sendMessage(ColorUtil.color("<gradient:#FF5F6D:#FFC371>&lСписок регионов AtomRegen (" + all.size() + "):</gradient>"));
        for (CuboidRegion reg : all) {
            sender.sendMessage(ColorUtil.color(" &8• &e" + reg.getName()
                    + " &7(" + reg.getWorldName() + ": [" + reg.getMinX() + "," + reg.getMinY() + "," + reg.getMinZ()
                    + " -> " + reg.getMaxX() + "," + reg.getMaxY() + "," + reg.getMaxZ() + "])"
                    + " &8| &fБлоков: &b" + reg.getVolume()
                    + " &8| &a" + reg.getRegenDelaySeconds() + "с"));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.color(getPrefix() + "&cИспользование: &e/ar info <имя>"));
            return;
        }

        String name = args[1];
        CuboidRegion reg = regionManager.getRegion(name);
        if (reg == null) {
            String msg = getMsg("region-not-found").replace("%region%", name);
            sender.sendMessage(ColorUtil.color(getPrefix() + msg));
            return;
        }

        sender.sendMessage(ColorUtil.color("<gradient:#FF5F6D:#FFC371>&lИнформация о регионе " + reg.getName() + ":</gradient>"));
        sender.sendMessage(ColorUtil.color(" &8• &7Мир: &e" + reg.getWorldName()));
        sender.sendMessage(ColorUtil.color(" &8• &7Координаты: &f" + reg.getMinX() + "," + reg.getMinY() + "," + reg.getMinZ()
                + " &8→ &f" + reg.getMaxX() + "," + reg.getMaxY() + "," + reg.getMaxZ()));
        sender.sendMessage(ColorUtil.color(" &8• &7Объем: &b" + reg.getVolume() + " блоков"));
        sender.sendMessage(ColorUtil.color(" &8• &7Задержка регенерации: &a" + reg.getRegenDelaySeconds() + " сек."));
        sender.sendMessage(ColorUtil.color(" &8• &7Дроп при ломании: " + (reg.isDropBlocksOnBreak() ? "&aВКЛ" : "&cВЫКЛ")));
        sender.sendMessage(ColorUtil.color(" &8• &7Дроп при взрыве: " + (reg.isDropBlocksOnExplode() ? "&aВКЛ" : "&cВЫКЛ")));
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        regionManager.loadRegions();
        regenEngine.start();
        sender.sendMessage(ColorUtil.color(getPrefix() + getMsg("reload-success")));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.color(getMsg("help-header")));
        sender.sendMessage(ColorUtil.color(getMsg("help-wand")));
        sender.sendMessage(ColorUtil.color(getMsg("help-pos1")));
        sender.sendMessage(ColorUtil.color(getMsg("help-pos2")));
        sender.sendMessage(ColorUtil.color(getMsg("help-create")));
        sender.sendMessage(ColorUtil.color(getMsg("help-delete")));
        sender.sendMessage(ColorUtil.color(getMsg("help-setdelay")));
        sender.sendMessage(ColorUtil.color(getMsg("help-reset")));
        sender.sendMessage(ColorUtil.color(getMsg("help-list")));
        sender.sendMessage(ColorUtil.color(getMsg("help-info")));
        sender.sendMessage(ColorUtil.color(getMsg("help-reload")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("atomregen.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subs = Arrays.asList("wand", "pos1", "pos2", "create", "delete", "setdelay", "reset", "list", "info", "reload");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("delete", "setdelay", "reset", "info").contains(sub)) {
                return regionManager.getAllRegions().stream()
                        .map(CuboidRegion::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if ("create".equals(sub)) {
                return List.of("<имя_региона>");
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if ("create".equals(sub) || "setdelay".equals(sub)) {
                return List.of("5", "10", "15", "30", "60", "120");
            }
        }

        return Collections.emptyList();
    }
}
