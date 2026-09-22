package me.ejyqyl.tituls.command;

import me.ejyqyl.tituls.AtomTitulsPlugin;
import me.ejyqyl.tituls.gui.TitulsMenu;
import me.ejyqyl.tituls.manager.ConfigManager;
import me.ejyqyl.tituls.manager.PlayerCache;
import me.ejyqyl.tituls.model.Titul;
import me.ejyqyl.tituls.model.TitulRarity;
import me.ejyqyl.tituls.model.TitulType;
import me.ejyqyl.tituls.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TitulsCommand implements CommandExecutor, TabCompleter {
    private final AtomTitulsPlugin plugin;

    public TitulsCommand(@NotNull AtomTitulsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasPermission(CommandSender sender, String action) {
        return sender.hasPermission("atomtituls." + action) || sender.hasPermission("stickhwtituls." + action);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        ConfigManager cfg = plugin.getConfigManager();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(TextUtil.toComponent(cfg.getMessage("only-players")));
                return true;
            }
            new TitulsMenu(plugin).open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            if (!hasPermission(sender, "reload")) {
                sender.sendMessage(TextUtil.toComponent(cfg.getMessage("no-permission")));
                return true;
            }
            try {
                plugin.reloadAll();
                sender.sendMessage(TextUtil.toComponent(cfg.getMessage("reload")));
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to reload plugin configs", ex);
                sender.sendMessage(TextUtil.toComponent(cfg.getMessage("reload-failed")));
            }
            return true;
        }

        if (sub.equals("give")) {
            if (!hasPermission(sender, "give")) {
                sender.sendMessage(TextUtil.toComponent(cfg.getMessage("no-permission")));
                return true;
            }
            handleGive(sender, args);
            return true;
        }

        if (sub.equals("take")) {
            if (!hasPermission(sender, "take")) {
                sender.sendMessage(TextUtil.toComponent(cfg.getMessage("no-permission")));
                return true;
            }
            handleTake(sender, args);
            return true;
        }

        // Viewing another player's titles: /tituls <player>
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.toComponent(cfg.getMessage("only-players")));
            return true;
        }

        openOtherTituls(player, args[0]);
        return true;
    }

    private void openOtherTituls(Player viewer, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = target.getUniqueId();
        String displayName = target.getName() != null ? target.getName() : targetName;

        plugin.getPlayerCache().loadSnapshot(targetUuid).thenAccept(data -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (viewer.isOnline()) {
                    new TitulsMenu(plugin, 1, targetUuid, displayName, true, data).open(viewer);
                }
            });
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.WARNING, "Failed to load titles for viewer " + displayName, ex);
            return null;
        });
    }

    private void handleGive(CommandSender sender, String[] args) {
        ConfigManager cfg = plugin.getConfigManager();
        if (args.length < 5) {
            sender.sendMessage(TextUtil.toComponent("§cИспользование: /tituls give <игрок> <titul|tag> <case|custom> <id> [отображение...]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(TextUtil.toComponent(cfg.getMessage("player-not-found").replace("{player}", args[1])));
            return;
        }

        String giveType = args[2].toLowerCase(); // titul or tag
        String mode = args[3].toLowerCase(); // case or custom

        if (mode.equals("custom")) {
            if (args.length < 6) {
                sender.sendMessage(TextUtil.toComponent("§cИспользование: /tituls give <игрок> <titul|tag> custom <id> <отображение>"));
                return;
            }

            String rawId = args[4].toLowerCase().replaceAll("[^a-z0-9_-]", "");
            if (rawId.isEmpty()) rawId = "titul";
            if (rawId.length() > 45) rawId = rawId.substring(0, 45);
            String titleId = "custom-" + rawId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

            String display = String.join(" ", Arrays.copyOfRange(args, 5, args.length));
            UUID targetUuid = target.getUniqueId();
            String playerName = target.getName();

            plugin.getTitleStorage().existsCustomTitle(titleId).thenAccept(exists -> {
                if (exists) {
                    sender.sendMessage(TextUtil.toComponent(cfg.getMessage("custom-id-exists").replace("{id}", titleId)));
                    return;
                }

                Titul customTitul = new Titul(titleId, display, TitulType.CUSTOM, TitulRarity.RARE, null);
                long now = System.currentTimeMillis();

                if (giveType.equals("titul")) {
                    PlayerCache cache = plugin.getPlayerCache();
                    if (cache.hasTitul(targetUuid, titleId)) {
                        sender.sendMessage(TextUtil.toComponent(cfg.getMessage("already-have").replace("{player}", playerName)));
                        return;
                    }

                    plugin.getTitleStorage().createCustomTitle(titleId, targetUuid, display, now)
                            .thenCompose(v -> cache.unlockTitul(targetUuid, customTitul, now))
                            .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                                sender.sendMessage(TextUtil.toComponent(cfg.getMessage("titul-give-sender")
                                        .replace("{titul}", display)
                                        .replace("{player}", playerName)));
                            })).exceptionally(ex -> {
                                handleFailure(sender, "Не удалось выдать кастомный титул " + titleId, ex);
                                return null;
                            });
                } else if (giveType.equals("tag")) {
                    plugin.getTitleStorage().createCustomTitle(titleId, targetUuid, display, now)
                            .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                                if (target.isOnline()) {
                                    giveTag(target, customTitul);
                                }
                                sender.sendMessage(TextUtil.toComponent(cfg.getMessage("tag-give-sender")
                                        .replace("{titul}", display)
                                        .replace("{player}", playerName)));
                            })).exceptionally(ex -> {
                                handleFailure(sender, "Не удалось выдать бирку кастомного титула " + titleId, ex);
                                return null;
                            });
                }
            });
            return;
        }

        if (mode.equals("case")) {
            String titleId = args[4];
            Titul template = plugin.getTitulManager().getTitul(titleId);
            if (template == null) {
                sender.sendMessage(TextUtil.toComponent(cfg.getMessage("titul-not-found").replace("{id}", titleId)));
                return;
            }

            UUID targetUuid = target.getUniqueId();
            String playerName = target.getName();

            if (giveType.equals("titul")) {
                PlayerCache cache = plugin.getPlayerCache();
                if (cache.hasTitul(targetUuid, titleId)) {
                    sender.sendMessage(TextUtil.toComponent(cfg.getMessage("already-have").replace("{player}", playerName)));
                    return;
                }

                cache.unlockTitul(targetUuid, template, System.currentTimeMillis()).thenRun(() -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(TextUtil.toComponent(cfg.getMessage("titul-give-sender")
                                .replace("{titul}", template.getName())
                                .replace("{player}", playerName)));
                    });
                }).exceptionally(ex -> {
                    handleFailure(sender, "Не удалось выдать титул " + titleId, ex);
                    return null;
                });
            } else if (giveType.equals("tag")) {
                giveTag(target, template);
                sender.sendMessage(TextUtil.toComponent(cfg.getMessage("tag-give-sender")
                        .replace("{titul}", template.getName())
                        .replace("{player}", playerName)));
            }
        }
    }

    private void handleTake(CommandSender sender, String[] args) {
        ConfigManager cfg = plugin.getConfigManager();
        if (args.length < 3) {
            sender.sendMessage(TextUtil.toComponent("§cИспользование: /tituls take <игрок> <id>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(TextUtil.toComponent(cfg.getMessage("player-not-found").replace("{player}", args[1])));
            return;
        }

        String titleId = args[2];
        UUID targetUuid = target.getUniqueId();
        String playerName = target.getName();

        PlayerCache cache = plugin.getPlayerCache();
        Titul owned = cache.getUnlockedTituls(targetUuid).stream()
                .filter(t -> t.getId().equalsIgnoreCase(titleId))
                .findFirst()
                .orElse(null);

        if (owned == null) {
            sender.sendMessage(TextUtil.toComponent(cfg.getMessage("not-have").replace("{player}", playerName)));
            return;
        }

        cache.revokeTitul(targetUuid, titleId).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(TextUtil.toComponent(cfg.getMessage("titul-take-sender")
                        .replace("{titul}", owned.getName())
                        .replace("{player}", playerName)));
            });
        }).exceptionally(ex -> {
            handleFailure(sender, "Не удалось забрать титул " + titleId, ex);
            return null;
        });
    }

    private void giveTag(Player player, Titul titul) {
        ItemStack tag = plugin.getTitulTagManager().createTagItem(titul);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(tag);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private void handleFailure(CommandSender sender, String message, Throwable ex) {
        plugin.getLogger().log(Level.WARNING, message, ex);
        Bukkit.getScheduler().runTask(plugin, () -> {
            sender.sendMessage(TextUtil.toComponent(plugin.getConfigManager().getMessage("command-failed")));
        });
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            if (hasPermission(sender, "give")) list.add("give");
            if (hasPermission(sender, "take")) list.add("take");
            if (hasPermission(sender, "reload")) list.add("reload");
            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }
            return filter(list, args[0]);
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("give") && hasPermission(sender, "give")) {
            if (args.length == 2) {
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
            }
            if (args.length == 3) {
                return filter(Arrays.asList("titul", "tag"), args[2]);
            }
            if (args.length == 4) {
                return filter(Arrays.asList("case", "custom"), args[3]);
            }
            if (args.length == 5) {
                if (args[3].equalsIgnoreCase("case")) {
                    return filter(plugin.getTitulManager().getAllTituls().stream().map(Titul::getId).collect(Collectors.toList()), args[4]);
                }
                if (args[3].equalsIgnoreCase("custom")) {
                    return Collections.singletonList("id");
                }
            }
            if (args.length == 6 && args[3].equalsIgnoreCase("custom")) {
                return Collections.singletonList("отображение");
            }
        }

        if (sub.equals("take") && hasPermission(sender, "take")) {
            if (args.length == 2) {
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
            }
            if (args.length == 3) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    return filter(plugin.getPlayerCache().getUnlockedTituls(target.getUniqueId()).stream().map(Titul::getId).collect(Collectors.toList()), args[2]);
                }
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> source, String current) {
        String lower = current.toLowerCase();
        return source.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
