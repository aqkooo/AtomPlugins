package me.ejyqyl.tituls.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class TitulsMenuHolder implements InventoryHolder {
    private final TitulsMenu menu;
    private Inventory inventory;

    public TitulsMenuHolder(@NotNull TitulsMenu menu) {
        this.menu = menu;
    }

    public void setInventory(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    @NotNull
    public TitulsMenu getMenu() {
        return menu;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
