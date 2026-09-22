package me.ejyqyl.tituls.listener;

import me.ejyqyl.tituls.AtomTitulsPlugin;
import me.ejyqyl.tituls.manager.ConfigManager;
import me.ejyqyl.tituls.manager.PlayerCache;
import me.ejyqyl.tituls.model.Titul;
import me.ejyqyl.tituls.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.logging.Level;

public class TagInteractListener implements Listener {
    private final AtomTitulsPlugin plugin;

    public TagInteractListener(AtomTitulsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack handItem = event.getItem();
        if (!plugin.getTitulTagManager().isTitulTag(handItem)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerCache cache = plugin.getPlayerCache();
        ConfigManager cfg = plugin.getConfigManager();

        Titul titul = plugin.getTitulTagManager().readTitulFromTag(handItem);
        if (titul == null) {
            return;
        }

        if (cache.hasTitul(uuid, titul.getId())) {
            player.sendMessage(TextUtil.toComponent(cfg.getMessage("already-have-tag")));
            return;
        }

        if (!cache.tryAcquireAction(uuid)) {
            return;
        }

        ItemStack rollback = handItem.asOne();
        if (handItem.getAmount() > 1) {
            handItem.setAmount(handItem.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        long obtainedAt = titul.getObtainedAt() != null ? titul.getObtainedAt() : System.currentTimeMillis();
        cache.unlockTitul(uuid, titul, obtainedAt).whenComplete((v, ex) -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                cache.releaseAction(uuid);
                if (!player.isOnline()) return;

                if (ex == null) {
                    player.sendMessage(TextUtil.toComponent(cfg.getMessage("using-tag")));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                } else {
                    plugin.getLogger().log(Level.WARNING, "Failed to unlock title from tag " + titul.getId(), ex);
                    player.getInventory().addItem(rollback).values().forEach(is ->
                            player.getWorld().dropItemNaturally(player.getLocation(), is)
                    );
                    player.sendMessage(TextUtil.toComponent(cfg.getMessage("error")));
                }
            });
        });
    }
}
