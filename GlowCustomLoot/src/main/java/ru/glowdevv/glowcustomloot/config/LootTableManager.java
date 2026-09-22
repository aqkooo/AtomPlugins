package ru.glowdevv.glowcustomloot.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.glowdevv.glowcustomloot.model.DimensionType;
import ru.glowdevv.glowcustomloot.model.LootItem;
import ru.glowdevv.glowcustomloot.model.LootMode;
import ru.glowdevv.glowcustomloot.model.StructureLootTable;
import ru.glowdevv.glowcustomloot.provider.ItemProviderRegistry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class LootTableManager {
    private final JavaPlugin plugin;
    private final ItemProviderRegistry providerRegistry;

    private final Map<String, StructureLootTable> tablesByLootKey = new ConcurrentHashMap<>();
    private final Map<String, StructureLootTable> tablesByDimAndKey = new ConcurrentHashMap<>();
    private final Map<String, StructureLootTable> tablesById = new ConcurrentHashMap<>();
    private final List<StructureLootTable> wildcardTables = new CopyOnWriteArrayList<>();

    public LootTableManager(@NotNull JavaPlugin plugin, @NotNull ItemProviderRegistry providerRegistry) {
        this.plugin = plugin;
        this.providerRegistry = providerRegistry;
    }

    public void loadAll() {
        tablesByLootKey.clear();
        tablesByDimAndKey.clear();
        tablesById.clear();
        wildcardTables.clear();

        File lootDir = new File(plugin.getDataFolder(), "loot");
        if (!lootDir.exists()) {
            lootDir.mkdirs();
        }
        saveDefaultLootFiles();

        loadDirectory(lootDir, null);
        plugin.getLogger().info("Loaded " + tablesById.size() + " custom structure loot tables.");
    }

    private void saveDefaultLootFiles() {
        String[] defaults = {
                "loot/overworld/desert_pyramid.yml",
                "loot/overworld/jungle_temple.yml",
                "loot/overworld/simple_dungeon.yml",
                "loot/overworld/abandoned_mineshaft.yml",
                "loot/overworld/woodland_mansion.yml",
                "loot/overworld/pillager_outpost.yml",
                "loot/overworld/ancient_city.yml",
                "loot/overworld/trial_chambers.yml",
                "loot/overworld/stronghold.yml",
                "loot/overworld/shipwreck.yml",
                "loot/overworld/buried_treasure.yml",
                "loot/overworld/underwater_ruin.yml",
                "loot/overworld/igloo.yml",
                "loot/overworld/ruined_portal.yml",
                "loot/overworld/village.yml",
                "loot/nether/nether_bridge.yml",
                "loot/nether/bastion_remnant.yml",
                "loot/nether/ruined_portal_nether.yml",
                "loot/the_end/end_city_treasure.yml"
        };
        for (String res : defaults) {
            try {
                File target = new File(plugin.getDataFolder(), res);
                if (!target.exists()) {
                    if (target.getParentFile() != null) {
                        target.getParentFile().mkdirs();
                    }
                    plugin.saveResource(res, false);
                }
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to save default loot file: " + res, ex);
            }
        }
    }

    private void loadDirectory(File dir, @Nullable DimensionType inferredDim) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                DimensionType dim = DimensionType.fromString(file.getName());
                loadDirectory(file, dim != null ? dim : inferredDim);
            } else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                loadTableFile(file, inferredDim);
            }
        }
    }

    private void loadTableFile(File file, @Nullable DimensionType inferredDim) {
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String id = file.getName().replace(".yml", "").replace(".yaml", "");
            String name = cfg.getString("name", id);

            String iconName = cfg.getString("icon", "CHEST");
            Material icon = Material.matchMaterial(iconName);
            if (icon == null) icon = Material.CHEST;

            List<String> lootKeys = new ArrayList<>();
            if (cfg.isList("loot_tables")) {
                lootKeys.addAll(cfg.getStringList("loot_tables"));
            } else if (cfg.isString("loot_table")) {
                lootKeys.add(cfg.getString("loot_table"));
            } else if (cfg.isList("loot_table")) {
                lootKeys.addAll(cfg.getStringList("loot_table"));
            }
            if (lootKeys.isEmpty()) {
                lootKeys.add("minecraft:chests/" + id);
            }

            DimensionType dim = inferredDim;
            if (dim == null) {
                dim = DimensionType.fromString(cfg.getString("dimension"));
                if (dim == null) {
                    String firstKey = lootKeys.get(0).toLowerCase();
                    if (firstKey.contains("nether") || id.contains("nether")) dim = DimensionType.NETHER;
                    else if (firstKey.contains("end") || id.contains("end")) dim = DimensionType.THE_END;
                    else dim = DimensionType.OVERWORLD;
                }
            }

            LootMode mode = LootMode.fromString(cfg.getString("mode", "MERGE"));
            int minRolls = cfg.getInt("rolls.min", 1);
            int maxRolls = cfg.getInt("rolls.max", 4);

            StructureLootTable table = new StructureLootTable(id, name, icon, dim, lootKeys, mode, minRolls, maxRolls, file);

            ConfigurationSection itemsSec = cfg.getConfigurationSection("items");
            if (itemsSec != null) {
                for (String itemKey : itemsSec.getKeys(false)) {
                    ConfigurationSection itemSec = itemsSec.getConfigurationSection(itemKey);
                    if (itemSec == null) continue;

                    String providerId = itemSec.getString("provider", "VANILLA");
                    double chance = itemSec.getDouble("chance", 25.0);
                    int minAmount = itemSec.getInt("min_amount", 1);
                    int maxAmount = itemSec.getInt("max_amount", 1);

                    LootItem item = new LootItem(itemKey, providerId, chance, minAmount, maxAmount,
                            providerRegistry.buildItem(itemSec));

                    table.addItem(item);
                }
            }

            table.setDirty(false);
            for (String k : table.getLootTableKeys()) {
                String lower = k.toLowerCase().trim();
                indexKey(lower, dim, table);
                if (lower.contains("*")) {
                    wildcardTables.add(table);
                }
            }
            tablesById.put(id.toLowerCase().trim(), table);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to load loot table file " + file.getName(), ex);
        }
    }

    private void indexKey(@NotNull String key, @NotNull DimensionType dim, @NotNull StructureLootTable table) {
        tablesByLootKey.put(key, table);
        tablesByDimAndKey.put(dim.name() + ":" + key, table);

        if (key.startsWith("minecraft:")) {
            String stripped = key.substring(10);
            tablesByLootKey.put(stripped, table);
            tablesByDimAndKey.put(dim.name() + ":" + stripped, table);

            if (stripped.startsWith("chests/")) {
                String pure = stripped.substring(7);
                tablesByLootKey.put(pure, table);
                tablesByDimAndKey.put(dim.name() + ":" + pure, table);
            }
        } else {
            String withNs = "minecraft:" + key;
            tablesByLootKey.put(withNs, table);
            tablesByDimAndKey.put(dim.name() + ":" + withNs, table);

            if (!key.startsWith("chests/")) {
                String withChests = "minecraft:chests/" + key;
                tablesByLootKey.put(withChests, table);
                tablesByDimAndKey.put(dim.name() + ":" + withChests, table);
            }
        }
    }

    @Nullable
    public StructureLootTable getTableByLootKey(@Nullable String lootKey) {
        return getTableByLootKey(lootKey, null);
    }

    @Nullable
    public StructureLootTable getTableByLootKey(@Nullable String lootKey, @Nullable DimensionType dim) {
        if (lootKey == null) return null;
        String lower = lootKey.toLowerCase().trim();

        // 1. Try dimension specific direct match
        if (dim != null) {
            StructureLootTable dimMatch = tablesByDimAndKey.get(dim.name() + ":" + lower);
            if (dimMatch != null) return dimMatch;

            if (lower.startsWith("minecraft:")) {
                dimMatch = tablesByDimAndKey.get(dim.name() + ":" + lower.substring(10));
                if (dimMatch != null) return dimMatch;
            } else {
                dimMatch = tablesByDimAndKey.get(dim.name() + ":minecraft:" + lower);
                if (dimMatch != null) return dimMatch;
            }
        }

        // 2. Direct lookup in global key map
        StructureLootTable direct = tablesByLootKey.get(lower);
        if (direct != null && (dim == null || direct.getDimension() == dim)) {
            return direct;
        }
        if (lower.startsWith("minecraft:")) {
            StructureLootTable match = tablesByLootKey.get(lower.substring(10));
            if (match != null && (dim == null || match.getDimension() == dim)) {
                return match;
            }
        } else {
            StructureLootTable match = tablesByLootKey.get("minecraft:" + lower);
            if (match != null && (dim == null || match.getDimension() == dim)) {
                return match;
            }
        }

        // 3. Wildcard matching (respecting dimension if present)
        for (StructureLootTable wildcard : wildcardTables) {
            if (dim != null && wildcard.getDimension() != dim) continue;
            for (String key : wildcard.getLootTableKeys()) {
                if (key.endsWith("*")) {
                    String prefix = key.substring(0, key.length() - 1);
                    if (lower.startsWith(prefix)) {
                        return wildcard;
                    }
                    String cleanPrefix = prefix.replace("minecraft:", "");
                    String cleanLower = lower.replace("minecraft:", "");
                    if (cleanLower.startsWith(cleanPrefix)) {
                        return wildcard;
                    }
                }
            }
        }

        // 4. Wildcard matching (ignoring dimension)
        for (StructureLootTable wildcard : wildcardTables) {
            for (String key : wildcard.getLootTableKeys()) {
                if (key.endsWith("*")) {
                    String cleanPrefix = key.substring(0, key.length() - 1).replace("minecraft:", "");
                    String cleanLower = lower.replace("minecraft:", "");
                    if (cleanLower.startsWith(cleanPrefix)) {
                        return wildcard;
                    }
                }
            }
        }

        // 5. Fallback: match without dimension restriction
        if (direct != null) {
            return direct;
        }
        return tablesByLootKey.get(lower);
    }

    @Nullable
    public StructureLootTable getTableById(@Nullable String id) {
        if (id == null) return null;
        String lower = id.toLowerCase().trim();
        StructureLootTable table = tablesById.get(lower);
        if (table != null) return table;
        for (StructureLootTable t : tablesById.values()) {
            if (t.getId().equalsIgnoreCase(lower) || t.getName().equalsIgnoreCase(lower)) {
                return t;
            }
        }
        return null;
    }

    @NotNull
    public List<StructureLootTable> getTablesForDimension(@NotNull DimensionType dimension) {
        List<StructureLootTable> list = new ArrayList<>();
        for (StructureLootTable table : tablesById.values()) {
            if (table.getDimension() == dimension) {
                list.add(table);
            }
        }
        return list;
    }

    public void saveTableSync(@NotNull StructureLootTable table) {
        File file = table.getFile();
        if (file == null) {
            File dir = new File(new File(plugin.getDataFolder(), "loot"), table.getDimension().getId());
            dir.mkdirs();
            file = new File(dir, table.getId() + ".yml");
            table.setFile(file);
        }

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("name", table.getName());
        cfg.set("icon", table.getIcon().name());
        if (table.getLootTableKeys().size() > 1) {
            cfg.set("loot_tables", table.getLootTableKeys());
        } else {
            cfg.set("loot_table", table.getLootTableKey());
        }
        cfg.set("mode", table.getMode().name());
        cfg.set("rolls.min", table.getMinRolls());
        cfg.set("rolls.max", table.getMaxRolls());

        ConfigurationSection itemsSec = cfg.createSection("items");
        for (LootItem item : table.getItems()) {
            ConfigurationSection itemSec = itemsSec.createSection(item.getId());
            itemSec.set("chance", item.getChance());
            itemSec.set("min_amount", item.getMinAmount());
            itemSec.set("max_amount", item.getMaxAmount());
            providerRegistry.serializeItem(item.getTemplate(), itemSec);
        }

        try {
            cfg.save(file);
            table.setDirty(false);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save loot table " + table.getId() + " to " + file.getName(), e);
        }
    }

    public CompletableFuture<Void> saveTableAsync(@NotNull StructureLootTable table) {
        return CompletableFuture.runAsync(() -> saveTableSync(table));
    }

    public void saveAllDirtySync() {
        for (StructureLootTable table : tablesByLootKey.values()) {
            if (table.isDirty()) {
                saveTableSync(table);
            }
        }
    }

    public CompletableFuture<Void> saveAllDirtyAsync() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (StructureLootTable table : tablesByLootKey.values()) {
            if (table.isDirty()) {
                futures.add(saveTableAsync(table));
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
}
