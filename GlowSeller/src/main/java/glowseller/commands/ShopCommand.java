package glowseller.commands;

import glowseller.Main;
import glowseller.menu.ShopMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShopCommand implements CommandExecutor {
    private final Main plugin;

    public ShopCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков!");
            return true;
        }

        if (!player.hasPermission("glowseller.shop")) {
            plugin.getMessageConfig().send(player, "commands.no_permission");
            return true;
        }

        if (plugin.getPlayerDataCache().isLoading(player.getUniqueId())) {
            player.sendMessage("§eПодождите, ваши данные еще загружаются...");
            return true;
        }

        new ShopMenu(plugin, player).open();
        return true;
    }
}
