package com.ejyqyl.atompvpbot.listener;

import com.ejyqyl.atompvpbot.arena.Arena;
import com.ejyqyl.atompvpbot.arena.ArenaManager;
import com.ejyqyl.atompvpbot.config.ConfigManager;
import com.ejyqyl.atompvpbot.fight.BotFight;
import com.ejyqyl.atompvpbot.fight.FightManager;
import com.ejyqyl.atompvpbot.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * Enforces arena protection, command whitelisting, and item security during fights.
 *
 * @author ejyqyl
 */
public class ArenaProtectionListener implements Listener {

    private final FightManager fightManager;
    private final ArenaManager arenaManager;
    private final ConfigManager configManager;

    public ArenaProtectionListener(FightManager fightManager, ArenaManager arenaManager, ConfigManager configManager) {
        this.fightManager = fightManager;
        this.arenaManager = arenaManager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (fightManager.isInFight(player)) {
            event.setCancelled(true);
            return;
        }

        Arena arena = arenaManager.getArenaAt(event.getBlock().getLocation());
        if (arena != null && !player.hasPermission("atompvpbot.admin")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (fightManager.isInFight(player)) {
            event.setCancelled(true);
            return;
        }

        Arena arena = arenaManager.getArenaAt(event.getBlock().getLocation());
        if (arena != null && !player.hasPermission("atompvpbot.admin")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (fightManager.isInFight(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (fightManager.isInFight(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        BotFight fight = fightManager.getFight(player);
        if (fight == null || fight.getState() == BotFight.State.ENDED) return;

        String rawCommand = event.getMessage().substring(1); // remove leading slash
        if (!configManager.isCommandAllowed(rawCommand)) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &cВо время боя разрешены только команды: &f" +
                    configManager.getConfig().getStringList("allowed-commands")));
        }
    }
}
