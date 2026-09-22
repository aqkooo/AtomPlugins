package ru.glowdevv.glowcustomloot.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.glowdevv.glowcustomloot.GlowCustomLootPlugin;
import ru.glowdevv.glowcustomloot.model.LootMode;
import ru.glowdevv.glowcustomloot.model.StructureLootTable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LootListener implements Listener {
    private final GlowCustomLootPlugin plugin;

    public LootListener(@NotNull GlowCustomLootPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLootGenerate(@NotNull LootGenerateEvent event) {
        if (event.getLootTable() == null) return;

        String key = event.getLootTable().getKey().toString().toLowerCase();
        ru.glowdevv.glowcustomloot.model.DimensionType dim = null;
        if (event.getWorld() != null) {
            dim = switch (event.getWorld().getEnvironment()) {
                case NETHER -> ru.glowdevv.glowcustomloot.model.DimensionType.NETHER;
                case THE_END -> ru.glowdevv.glowcustomloot.model.DimensionType.THE_END;
                default -> ru.glowdevv.glowcustomloot.model.DimensionType.OVERWORLD;
            };
        }

        boolean debug = plugin.getConfigManager().isDebug();
        if (debug) {
            plugin.getLogger().info("[DEBUG] LootGenerateEvent fired for: " + key
                    + " (world: " + (event.getWorld() != null ? event.getWorld().getName() : "null")
                    + ", dim: " + (dim != null ? dim.getId() : "unknown") + ")");
        }

        StructureLootTable table = plugin.getLootTableManager().getTableByLootKey(key, dim);
        if (table == null) {
            if (debug) {
                plugin.getLogger().info("[DEBUG] No custom loot table configured for key: " + key);
            }
            return;
        }

        int maxItemsCap = plugin.getConfigManager().getMaxItemsPerChest();
        List<ItemStack> customItems = table.generateLoot(ThreadLocalRandom.current(), maxItemsCap);

        List<ItemStack> loot = new java.util.ArrayList<>(event.getLoot());

        if (table.getMode() == LootMode.REPLACE) {
            loot.clear();
            loot.addAll(customItems);
        } else {
            // MERGE mode: preserve custom items and make room if needed
            int spaceForVanilla = Math.max(0, maxItemsCap - customItems.size());
            while (loot.size() > spaceForVanilla) {
                loot.remove(loot.size() - 1);
            }
            loot.addAll(customItems);
        }

        // Final safety cap
        while (loot.size() > maxItemsCap) {
            loot.remove(loot.size() - 1);
        }

        // Apply updated loot back to event
        event.setLoot(loot);

        if (debug) {
            plugin.getLogger().info("[DEBUG] Intercepted LootGenerateEvent for " + key
                    + " [Table: " + table.getId() + "] -> Added " + customItems.size()
                    + " custom items (mode: " + table.getMode() + ", total in container: " + loot.size() + ")");
        }
    }
}
