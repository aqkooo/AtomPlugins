package com.ejyqyl.glowcmd.command.player;

import com.ejyqyl.glowcmd.GlowCMD;
import com.ejyqyl.glowcmd.config.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /heal [player] command. Restores health, food, extinguishes fire, and clears negative effects.
 *
 * @author ejyqyl
 */
public final class HealCommand implements CommandExecutor, TabCompleter {

    private final GlowCMD plugin;
    private final MessageManager messageManager;

    public HealCommand(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                messageManager.send(sender, "player-only");
                return true;
            }
            if (!player.hasPermission("glowcmd.heal")) {
                messageManager.send(player, "no-permission");
                return true;
            }

            healPlayer(player);
            messageManager.send(player, "heal.self");
            return true;
        }

        // Heal another player
        if (!sender.hasPermission("glowcmd.heal.others")) {
            messageManager.send(sender, "no-permission");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            messageManager.send(sender, "player-not-found", Map.of("{TARGET}", args[0]));
            return true;
        }

        healPlayer(target);
        messageManager.send(sender, "heal.other", Map.of("{TARGET}", target.getName()));
        messageManager.send(target, "heal.by-other", Map.of("{SENDER}", sender.getName()));
        return true;
    }

    private void healPlayer(@NotNull Player player) {
        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = attr != null ? attr.getValue() : 20.0;
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setFireTicks(0);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            // Remove negative effects
            player.removePotionEffect(effect.getType());
        }
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("glowcmd.heal.others")) {
            return null; // Bukkit default player name completion
        }
        return Collections.emptyList();
    }
}
