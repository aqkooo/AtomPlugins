package glowseller.commands;

import glowseller.Main;
import glowseller.events.PointsUpdateEvent;
import glowseller.menu.MainMenu;
import glowseller.models.PlayerData;
import glowseller.models.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GlowSellerAdminCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public GlowSellerAdminCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (player.hasPermission("glowseller.use")) {
                    new MainMenu(plugin, player).open();
                    return true;
                }
            }
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("reload")) {
            if (!sender.hasPermission("glowseller.admin")) {
                plugin.getMessageConfig().send(sender, "commands.no_permission");
                return true;
            }

            plugin.reloadAll();
            plugin.getMessageConfig().send(sender, "commands.reloaded");
            return true;
        }

        if (sub.equals("admin")) {
            if (!sender.hasPermission("glowseller.admin")) {
                plugin.getMessageConfig().send(sender, "commands.no_permission");
                return true;
            }

            if (args.length < 2) {
                sendHelp(sender);
                return true;
            }

            String action = args[1].toLowerCase();
            switch (action) {
                case "give" -> {
                    if (args.length < 4) {
                        sender.sendMessage("§eИспользование: /glowseller admin give <игрок> <очки>");
                        return true;
                    }
                    Player target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) {
                        plugin.getMessageConfig().send(sender, "commands.player_not_found", "%player%", args[2]);
                        return true;
                    }

                    long amount;
                    try {
                        amount = Long.parseLong(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cНекорректное число очков!");
                        return true;
                    }

                    if (amount <= 0) {
                        sender.sendMessage("§cКоличество очков должно быть больше нуля!");
                        return true;
                    }

                    PlayerData data = plugin.getPlayerDataCache().getOrCreate(target.getUniqueId());
                    long oldPoints = data.getPoints();
                    data.addPoints(amount);

                    Bukkit.getPluginManager().callEvent(new PointsUpdateEvent(target, oldPoints, data.getPoints(), PointsUpdateEvent.Cause.ADMIN_GIVE));

                    plugin.getMessageConfig().send(sender, "commands.points_given",
                            "%player%", target.getName(),
                            "%amount%", plugin.getNumberFormatManager().formatNumber(amount)
                    );
                    plugin.getMessageConfig().send(target, "commands.points_received",
                            "%amount%", plugin.getNumberFormatManager().formatNumber(amount)
                    );
                    return true;
                }
                case "take" -> {
                    if (args.length < 4) {
                        sender.sendMessage("§eИспользование: /glowseller admin take <игрок> <очки>");
                        return true;
                    }
                    Player target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) {
                        plugin.getMessageConfig().send(sender, "commands.player_not_found", "%player%", args[2]);
                        return true;
                    }

                    long amount;
                    try {
                        amount = Long.parseLong(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cНекорректное число очков!");
                        return true;
                    }

                    if (amount <= 0) {
                        sender.sendMessage("§cКоличество очков должно быть больше нуля!");
                        return true;
                    }

                    PlayerData data = plugin.getPlayerDataCache().getOrCreate(target.getUniqueId());
                    long oldPoints = data.getPoints();
                    if (!data.takePoints(amount)) {
                        sender.sendMessage("§cУ игрока недостаточно очков для списания! (Текущий баланс: " + data.getPoints() + ")");
                        return true;
                    }

                    Bukkit.getPluginManager().callEvent(new PointsUpdateEvent(target, oldPoints, data.getPoints(), PointsUpdateEvent.Cause.ADMIN_TAKE));

                    plugin.getMessageConfig().send(sender, "commands.points_taken",
                            "%player%", target.getName(),
                            "%amount%", plugin.getNumberFormatManager().formatNumber(amount)
                    );
                    return true;
                }
                case "booster" -> {
                    if (args.length >= 3 && args[2].equalsIgnoreCase("give")) {
                        if (args.length < 5) {
                            sender.sendMessage("§eИспользование: /glowseller admin booster give <игрок> <ключ_бустера> [длительность_сек]");
                            return true;
                        }
                        Player target = Bukkit.getPlayerExact(args[3]);
                        if (target == null) {
                            plugin.getMessageConfig().send(sender, "commands.player_not_found", "%player%", args[3]);
                            return true;
                        }

                        String boosterKey = args[4];
                        ShopItem item = plugin.getShopConfig().getItem(boosterKey);
                        double multiplier = item != null ? item.getMultiplier() : 2.0;
                        long duration = item != null && item.getDurationSeconds() > 0 ? item.getDurationSeconds() : 3600L;

                        if (args.length >= 6) {
                            try {
                                duration = Long.parseLong(args[5]);
                            } catch (NumberFormatException ignored) {}
                        }

                        plugin.getBoosterManager().applyBooster(target, boosterKey, multiplier, duration);
                        plugin.getMessageConfig().send(sender, "commands.booster_given",
                                "%player%", target.getName(),
                                "%booster%", boosterKey,
                                "%duration%", plugin.getBoosterManager().formatTime(duration)
                        );
                        plugin.getMessageConfig().send(target, "commands.booster_received",
                                "%booster%", boosterKey,
                                "%multiplier%", String.format("%.1f", multiplier),
                                "%duration%", plugin.getBoosterManager().formatTime(duration)
                        );
                        return true;
                    }
                }
            }
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m----------------§r &#54A0F4&lGlowSeller §8§m----------------");
        sender.sendMessage("§f/sell §7(алиасы buyer, seller) — меню скупщика");
        sender.sendMessage("§f/shop §7(алиас gshop) — магазин за очки");
        if (sender.hasPermission("glowseller.admin")) {
            sender.sendMessage("§f/glowseller reload §7— перезагрузить конфигурацию");
            sender.sendMessage("§f/glowseller admin give <игрок> <очки> §7— выдать очки");
            sender.sendMessage("§f/glowseller admin take <игрок> <очки> §7— забрать очки");
            sender.sendMessage("§f/glowseller admin booster give <игрок> <ключ> [сек] §7— выдать бустер");
        }
        sender.sendMessage("§8§m-------------------------------------------");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("menu"));
            if (sender.hasPermission("glowseller.admin")) {
                subs.addAll(Arrays.asList("reload", "admin", "help"));
            }
            return filter(subs, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("glowseller.admin")) {
            return filter(Arrays.asList("give", "take", "booster"), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("glowseller.admin")) {
            if (args[1].equalsIgnoreCase("booster")) {
                return filter(List.of("give"), args[2]);
            }
            return null; // players list
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("glowseller.admin")) {
            if (args[1].equalsIgnoreCase("booster") && args[2].equalsIgnoreCase("give")) {
                return null; // players list
            }
            if (args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("take")) {
                return List.of("100", "500", "1000", "5000");
            }
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("booster") && args[2].equalsIgnoreCase("give")) {
            return filter(new ArrayList<>(plugin.getShopConfig().getCategories().values().stream()
                    .flatMap(c -> c.getItems().values().stream())
                    .filter(i -> i.getType() == ShopItem.Type.BOOSTER)
                    .map(ShopItem::getKey)
                    .collect(Collectors.toList())), args[4]);
        }

        return completions;
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        return list.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
