package glowseller.menu;

import glowseller.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractMenu implements InventoryHolder {
    protected final Main plugin;
    protected final Player player;
    protected Inventory inventory;
    protected final Map<Integer, Consumer<InventoryClickEvent>> slotActions = new HashMap<>();

    public AbstractMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public abstract String getTitle();

    public abstract int getSize();

    public abstract void build();

    public void open() {
        this.inventory = Bukkit.createInventory(this, getSize(), getTitle());
        this.slotActions.clear();
        build();
        player.openInventory(this.inventory);
    }

    public void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> onClick) {
        if (slot < 0 || slot >= getSize()) return;
        inventory.setItem(slot, item);
        if (onClick != null) {
            slotActions.put(slot, onClick);
        } else {
            slotActions.remove(slot);
        }
    }

    public void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    public void fill(ItemStack filler) {
        for (int i = 0; i < getSize(); i++) {
            if (inventory.getItem(i) == null) {
                setItem(i, filler);
            }
        }
    }

    public void fillSlots(Collection<Integer> slots, ItemStack filler) {
        if (slots == null) return;
        for (int slot : slots) {
            if (slot >= 0 && slot < getSize()) {
                setItem(slot, filler);
            }
        }
    }

    public boolean isInteractive() {
        return false;
    }

    public boolean isSlotInteractive(int rawSlot) {
        return false;
    }

    public void handleDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        // Can be overridden by subclasses
    }

    public void onContentsChanged() {
        // Can be overridden by subclasses
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < getSize()) {
            Consumer<InventoryClickEvent> action = slotActions.get(rawSlot);
            if (action != null) {
                action.accept(event);
            }
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        // Can be overridden by subclasses
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}
