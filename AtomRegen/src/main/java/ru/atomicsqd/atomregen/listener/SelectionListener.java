package ru.atomicsqd.atomregen.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ru.atomicsqd.atomregen.AtomRegen;
import ru.atomicsqd.atomregen.service.SelectionService;
import ru.atomicsqd.atomregen.util.ColorUtil;

/**
 * Handles wand interaction events for selecting Pos1 and Pos2.
 */
public class SelectionListener implements Listener {

    private final AtomRegen plugin;
    private final SelectionService selectionService;

    public SelectionListener(AtomRegen plugin, SelectionService selectionService) {
        this.plugin = plugin;
        this.selectionService = selectionService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onWandInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = event.getItem();
        if (!selectionService.isWand(item)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("atomregen.admin")) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Action action = event.getAction();
        Location loc = clicked.getLocation();
        String prefix = plugin.getConfig().getString("prefix", "");

        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            selectionService.setPos1(player, loc);
            String msg = plugin.getConfig().getString("messages.pos1-set", "&aТочка 1 установлена!")
                    .replace("%x%", String.valueOf(loc.getBlockX()))
                    .replace("%y%", String.valueOf(loc.getBlockY()))
                    .replace("%z%", String.valueOf(loc.getBlockZ()))
                    .replace("%world%", loc.getWorld().getName());
            player.sendMessage(ColorUtil.color(prefix + msg));
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            selectionService.setPos2(player, loc);
            String msg = plugin.getConfig().getString("messages.pos2-set", "&bТочка 2 установлена!")
                    .replace("%x%", String.valueOf(loc.getBlockX()))
                    .replace("%y%", String.valueOf(loc.getBlockY()))
                    .replace("%z%", String.valueOf(loc.getBlockZ()))
                    .replace("%world%", loc.getWorld().getName());
            player.sendMessage(ColorUtil.color(prefix + msg));
        }
    }
}
