package ru.glowdevv.glowcustomloot.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MenuHolder implements InventoryHolder {
    private final String menuId;
    private Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();
    private Consumer<InventoryClickEvent> defaultClickHandler;
    private Consumer<InventoryClickEvent> playerInventoryClickHandler;
    private Consumer<InventoryCloseEvent> closeHandler;

    public MenuHolder(@NotNull String menuId) {
        this.menuId = menuId;
    }

    public void setInventory(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @NotNull
    public String getMenuId() {
        return menuId;
    }

    public void setHandler(int slot, @Nullable Consumer<InventoryClickEvent> handler) {
        if (handler != null) {
            clickHandlers.put(slot, handler);
        } else {
            clickHandlers.remove(slot);
        }
    }

    public void setDefaultClickHandler(@Nullable Consumer<InventoryClickEvent> defaultClickHandler) {
        this.defaultClickHandler = defaultClickHandler;
    }

    public void setPlayerInventoryClickHandler(@Nullable Consumer<InventoryClickEvent> playerInventoryClickHandler) {
        this.playerInventoryClickHandler = playerInventoryClickHandler;
    }

    public void setCloseHandler(@Nullable Consumer<InventoryCloseEvent> closeHandler) {
        this.closeHandler = closeHandler;
    }

    public void handleClick(@NotNull InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < event.getView().getTopInventory().getSize()) {
            Consumer<InventoryClickEvent> handler = clickHandlers.get(rawSlot);
            if (handler != null) {
                handler.accept(event);
            } else if (defaultClickHandler != null) {
                defaultClickHandler.accept(event);
            }
        } else if (rawSlot >= event.getView().getTopInventory().getSize()) {
            if (playerInventoryClickHandler != null) {
                playerInventoryClickHandler.accept(event);
            }
        }
    }

    public void handleClose(@NotNull InventoryCloseEvent event) {
        if (closeHandler != null) {
            closeHandler.accept(event);
        }
    }
}
