package com.ejyqyl.atomduels.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Interface for all interactive AtomDuels GUIs.
 * Authored by ejyqyl.
 */
public interface AtomMenu extends InventoryHolder {

    void handleClick(InventoryClickEvent event);
}
