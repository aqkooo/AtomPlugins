package com.ejyqyl.atompvpbot.gui;

import com.ejyqyl.atompvpbot.util.ColorUtil;
import com.ejyqyl.atompvpbot.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base GUI container with click dispatching and clean styling.
 *
 * @author ejyqyl
 */
public abstract class AtomMenu implements InventoryHolder {

    protected final Player player;
    protected final int size;
    protected final String title;
    protected final Inventory inventory;
    protected final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    public AtomMenu(Player player, int size, String title) {
        this.player = player;
        this.size = size;
        this.title = title;
        this.inventory = Bukkit.createInventory(this, size, ColorUtil.parse(title));
    }

    public abstract void initialize();

    public void open() {
        initialize();
        player.openInventory(inventory);
    }

    public void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        } else {
            actions.remove(slot);
        }
    }

    public void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    public void fillBorder(ItemStack item) {
        int rows = size / 9;
        for (int c = 0; c < 9; c++) {
            setItem(c, item);
            setItem((rows - 1) * 9 + c, item);
        }
        for (int r = 1; r < rows - 1; r++) {
            setItem(r * 9, item);
            setItem(r * 9 + 8, item);
        }
    }

    public void fillEmpty(ItemStack item) {
        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                setItem(i, item);
            }
        }
    }

    protected ItemStack createGlass(Material material) {
        return new ItemBuilder(material)
                .name("&7")
                .hideFlags()
                .build();
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Consumer<InventoryClickEvent> action = actions.get(event.getRawSlot());
        if (action != null) {
            action.accept(event);
        }
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
