package com.ejyqyl.atompvpbot.listener;

import com.ejyqyl.atompvpbot.gui.AtomMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Dispatches GUI inventory clicks to AtomMenu holders.
 *
 * @author ejyqyl
 */
public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof AtomMenu menu) {
            menu.handleClick(event);
        }
    }
}
