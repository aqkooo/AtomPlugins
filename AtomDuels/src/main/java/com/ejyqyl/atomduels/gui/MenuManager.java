package com.ejyqyl.atomduels.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Universal inventory click router for AtomDuels GUIs.
 * Authored by ejyqyl.
 */
public class MenuManager implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof AtomMenu menu) {
            menu.handleClick(event);
        }
    }
}
