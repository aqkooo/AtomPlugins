package glowseller.commands;

import glowseller.Main;
import glowseller.menu.AutoSellMenu;
import glowseller.menu.SellMenu;
import glowseller.menu.ShopMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SellCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public SellCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков!");
            return true;
        }

        if (!player.hasPermission("glowseller.use")) {
            plugin.getMessageConfig().send(player, "commands.no_permission");
            return true;
        }

        if (plugin.getPlayerDataCache().isLoading(player.getUniqueId())) {
            player.sendMessage("§eПодождите, ваши данные еще загружаются...");
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            if (sub.equals("auto") || sub.equals("filter")) {
                new AutoSellMenu(plugin, player).open();
                return true;
            }
            if (sub.equals("shop")) {
                new ShopMenu(plugin, player).open();
                return true;
            }
        }

        new SellMenu(plugin, player).open();
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = Arrays.asList("auto", "shop");
            String prefix = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String s : list) {
                if (s.startsWith(prefix)) result.add(s);
            }
            return result;
        }
        return Collections.emptyList();
    }
}
