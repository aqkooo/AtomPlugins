package ru.glowdevv.glowcustomloot.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class StructureLootTable {
    private final String id;
    private String name;
    private Material icon;
    private DimensionType dimension;
    private final List<String> lootTableKeys = new CopyOnWriteArrayList<>();
    private LootMode mode;
    private int minRolls;
    private int maxRolls;
    private final List<LootItem> items = new CopyOnWriteArrayList<>();
    private File file;
    private volatile boolean dirty = false;

    public StructureLootTable(@NotNull String id, @NotNull String name, @NotNull Material icon,
                              @NotNull DimensionType dimension, @NotNull String lootTableKey,
                              @NotNull LootMode mode, int minRolls, int maxRolls, @Nullable File file) {
        this(id, name, icon, dimension, Collections.singletonList(lootTableKey), mode, minRolls, maxRolls, file);
    }

    public StructureLootTable(@NotNull String id, @NotNull String name, @NotNull Material icon,
                              @NotNull DimensionType dimension, @NotNull List<String> lootTableKeys,
                              @NotNull LootMode mode, int minRolls, int maxRolls, @Nullable File file) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.dimension = dimension;
        if (lootTableKeys != null) {
            for (String k : lootTableKeys) {
                if (k != null && !k.isBlank()) {
                    this.lootTableKeys.add(k.trim().toLowerCase());
                }
            }
        }
        this.mode = mode;
        this.minRolls = Math.max(1, minRolls);
        this.maxRolls = Math.max(this.minRolls, maxRolls);
        this.file = file;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
        this.dirty = true;
    }

    @NotNull
    public Material getIcon() {
        return icon;
    }

    public void setIcon(@NotNull Material icon) {
        this.icon = icon;
        this.dirty = true;
    }

    @NotNull
    public DimensionType getDimension() {
        return dimension;
    }

    public void setDimension(@NotNull DimensionType dimension) {
        this.dimension = dimension;
        this.dirty = true;
    }

    @NotNull
    public String getLootTableKey() {
        if (lootTableKeys.isEmpty()) {
            return "minecraft:chests/" + id;
        }
        return lootTableKeys.get(0);
    }

    @NotNull
    public List<String> getLootTableKeys() {
        return lootTableKeys;
    }

    public void setLootTableKey(@NotNull String lootTableKey) {
        this.lootTableKeys.clear();
        if (!lootTableKey.isBlank()) {
            this.lootTableKeys.add(lootTableKey.trim().toLowerCase());
        }
        this.dirty = true;
    }

    public void setLootTableKeys(@NotNull List<String> keys) {
        this.lootTableKeys.clear();
        for (String k : keys) {
            if (k != null && !k.isBlank()) {
                this.lootTableKeys.add(k.trim().toLowerCase());
            }
        }
        this.dirty = true;
    }

    public void addLootTableKey(@NotNull String key) {
        if (!key.isBlank()) {
            this.lootTableKeys.add(key.trim().toLowerCase());
            this.dirty = true;
        }
    }

    @NotNull
    public LootMode getMode() {
        return mode;
    }

    public void setMode(@NotNull LootMode mode) {
        this.mode = mode;
        this.dirty = true;
    }

    public int getMinRolls() {
        return minRolls;
    }

    public void setMinRolls(int minRolls) {
        this.minRolls = Math.max(1, minRolls);
        if (this.maxRolls < this.minRolls) {
            this.maxRolls = this.minRolls;
        }
        this.dirty = true;
    }

    public int getMaxRolls() {
        return maxRolls;
    }

    public void setMaxRolls(int maxRolls) {
        this.maxRolls = Math.max(this.minRolls, maxRolls);
        this.dirty = true;
    }

    @NotNull
    public List<LootItem> getItems() {
        return items;
    }

    public void addItem(@NotNull LootItem item) {
        items.add(item);
        this.dirty = true;
    }

    public boolean removeItem(@NotNull String itemId) {
        boolean removed = items.removeIf(i -> i.getId().equalsIgnoreCase(itemId));
        if (removed) {
            this.dirty = true;
        }
        return removed;
    }

    @Nullable
    public LootItem getItem(@NotNull String itemId) {
        for (LootItem item : items) {
            if (item.getId().equalsIgnoreCase(itemId)) {
                return item;
            }
        }
        return null;
    }

    @NotNull
    public String nextAvailableId() {
        int highest = 0;
        for (LootItem item : items) {
            try {
                int val = Integer.parseInt(item.getId());
                if (val > highest) highest = val;
            } catch (NumberFormatException ignored) {
            }
        }
        return String.valueOf(highest + 1);
    }

    @Nullable
    public File getFile() {
        return file;
    }

    public void setFile(@Nullable File file) {
        this.file = file;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * Rolls custom items for this loot table.
     */
    @NotNull
    public List<ItemStack> generateLoot(@NotNull Random random, int maxItemsCap) {
        if (items.isEmpty() || maxItemsCap <= 0) {
            return Collections.emptyList();
        }

        int targetRolls = minRolls == maxRolls
                ? minRolls
                : minRolls + random.nextInt(maxRolls - minRolls + 1);
        targetRolls = Math.min(targetRolls, maxItemsCap);

        List<ItemStack> generated = new ArrayList<>();
        List<LootItem> pool = new ArrayList<>(items);

        int maxAttempts = Math.max(targetRolls * 5, 10);
        int attempts = 0;

        while (generated.size() < targetRolls && attempts < maxAttempts && !pool.isEmpty()) {
            attempts++;
            Collections.shuffle(pool, random);
            boolean anyAddedInPass = false;
            for (LootItem item : pool) {
                if (generated.size() >= targetRolls || generated.size() >= maxItemsCap) {
                    break;
                }
                ItemStack drop = item.roll(random);
                if (drop != null && drop.getType() != Material.AIR && drop.getType() != Material.CAVE_AIR && drop.getType() != Material.VOID_AIR && drop.getAmount() > 0) {
                    generated.add(drop);
                    anyAddedInPass = true;
                }
            }
            if (!anyAddedInPass && attempts > 3) {
                break;
            }
        }

        return generated;
    }
}
