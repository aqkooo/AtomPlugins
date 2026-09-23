package glowseller.managers;

import glowseller.Main;
import glowseller.models.item.Item;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemManager {
    private final Main plugin;
    private final Map<Material, List<Item>> materialIndex = new EnumMap<>(Material.class);

    public ItemManager(Main plugin) {
        this.plugin = plugin;
        rebuildIndex();
    }

    public synchronized void rebuildIndex() {
        materialIndex.clear();
        Map<String, Item> items = plugin.getItemsConfig().getItems();
        for (Item item : items.values()) {
            if (item.getMaterial() != null) {
                materialIndex.computeIfAbsent(item.getMaterial(), k -> new ArrayList<>()).add(item);
            }
        }
    }

    public Item findMatchingItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }

        List<Item> candidates = materialIndex.get(stack.getType());
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        for (Item item : candidates) {
            if (item.matches(stack)) {
                return item;
            }
        }

        return null;
    }

    public boolean isSellable(ItemStack stack) {
        return findMatchingItem(stack) != null;
    }

    public Map<String, Item> getItems() {
        return plugin.getItemsConfig().getItems();
    }
}
