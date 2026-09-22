package ru.glowdevv.glowcustomloot.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.glowdevv.glowcustomloot.GlowCustomLootPlugin;
import ru.glowdevv.glowcustomloot.gui.DimensionMenu;
import ru.glowdevv.glowcustomloot.util.TextUtil;

public class EditLootCommand implements CommandExecutor {
    private final GlowCustomLootPlugin plugin;

    public EditLootCommand(@NotNull GlowCustomLootPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("only-players"));
            return true;
        }

        if (!player.hasPermission("glowcustomloot.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        new DimensionMenu(plugin).open(player);
        return true;
    }
}
