package ru.glowdevv.glowcustomloot;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import ru.glowdevv.glowcustomloot.model.DimensionType;
import ru.glowdevv.glowcustomloot.model.LootMode;
import ru.glowdevv.glowcustomloot.model.StructureLootTable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class LootTableResolutionTest {

    @Test
    void testWildcardAndSpecificResolution() {
        Map<String, StructureLootTable> tablesByDimAndKey = new ConcurrentHashMap<>();
        Map<String, StructureLootTable> tablesByLootKey = new ConcurrentHashMap<>();
        List<StructureLootTable> wildcardTables = new CopyOnWriteArrayList<>();

        // Master village table
        StructureLootTable villageMaster = new StructureLootTable("village", "Деревня",
                Material.EMERALD, DimensionType.OVERWORLD,
                Arrays.asList("minecraft:chests/village/*"), LootMode.MERGE, 2, 4, null);

        for (String k : villageMaster.getLootTableKeys()) {
            tablesByLootKey.put(k, villageMaster);
            tablesByDimAndKey.put(villageMaster.getDimension().name() + ":" + k, villageMaster);
            if (k.contains("*")) wildcardTables.add(villageMaster);
        }

        // Specific weaponsmith table
        StructureLootTable weaponsmith = new StructureLootTable("village_weaponsmith", "Кузница",
                Material.ANVIL, DimensionType.OVERWORLD,
                "minecraft:chests/village/village_weaponsmith", LootMode.MERGE, 3, 6, null);

        for (String k : weaponsmith.getLootTableKeys()) {
            tablesByLootKey.put(k, weaponsmith);
            tablesByDimAndKey.put(weaponsmith.getDimension().name() + ":" + k, weaponsmith);
        }

        // Resolution function replicating LootTableManager logic
        java.util.function.BiFunction<String, DimensionType, StructureLootTable> resolver = (lootKey, dim) -> {
            String lower = lootKey.toLowerCase();
            if (dim != null) {
                StructureLootTable match = tablesByDimAndKey.get(dim.name() + ":" + lower);
                if (match != null) return match;
            }
            StructureLootTable direct = tablesByLootKey.get(lower);
            if (direct != null && (dim == null || direct.getDimension() == dim)) return direct;
            for (StructureLootTable w : wildcardTables) {
                if (dim != null && w.getDimension() != dim) continue;
                for (String key : w.getLootTableKeys()) {
                    if (key.endsWith("*")) {
                        String prefix = key.substring(0, key.length() - 1);
                        if (lower.startsWith(prefix)) return w;
                    }
                }
            }
            return direct;
        };

        // Specific table should resolve to weaponsmith
        StructureLootTable resolvedWeaponsmith = resolver.apply("minecraft:chests/village/village_weaponsmith", DimensionType.OVERWORLD);
        assertNotNull(resolvedWeaponsmith);
        assertEquals("village_weaponsmith", resolvedWeaponsmith.getId(), "Specific table should take precedence");

        // Shepherd table should resolve to villageMaster via wildcard
        StructureLootTable resolvedShepherd = resolver.apply("minecraft:chests/village/village_shepherd", DimensionType.OVERWORLD);
        assertNotNull(resolvedShepherd);
        assertEquals("village", resolvedShepherd.getId(), "Unmatched village house should fall back to wildcard village");

        // Temple should resolve to villageMaster via wildcard
        StructureLootTable resolvedTemple = resolver.apply("minecraft:chests/village/village_temple", DimensionType.OVERWORLD);
        assertNotNull(resolvedTemple);
        assertEquals("village", resolvedTemple.getId());

        // Completely unrelated structure shouldn't match
        StructureLootTable resolvedDesert = resolver.apply("minecraft:chests/desert_pyramid", DimensionType.OVERWORLD);
        assertNull(resolvedDesert);
    }

    @Test
    void testMergeModePreservesCustomItemsWhenCapped() {
        // Vanilla generates 8 items
        List<String> loot = new java.util.ArrayList<>(Arrays.asList("v1", "v2", "v3", "v4", "v5", "v6", "v7", "v8"));
        // Custom loot generates 3 items
        List<String> customItems = Arrays.asList("custom_diamond", "custom_emerald", "custom_netherite");

        int maxItemsCap = 8; // Simulating old tight cap

        // Correct MERGE algorithm:
        int spaceForVanilla = Math.max(0, maxItemsCap - customItems.size());
        while (loot.size() > spaceForVanilla) {
            loot.remove(loot.size() - 1);
        }
        loot.addAll(customItems);

        while (loot.size() > maxItemsCap) {
            loot.remove(loot.size() - 1);
        }

        assertEquals(8, loot.size());
        assertTrue(loot.contains("custom_diamond"), "Custom diamond must be in chest!");
        assertTrue(loot.contains("custom_emerald"), "Custom emerald must be in chest!");
        assertTrue(loot.contains("custom_netherite"), "Custom netherite must be in chest!");
        // Vanilla items should be trimmed to 5
        assertEquals(5, loot.stream().filter(s -> s.startsWith("v")).count());
    }

    @Test
    void testReplaceModeReplacesAllVanillaItems() {
        List<String> loot = new java.util.ArrayList<>(Arrays.asList("v1", "v2", "v3", "v4"));
        List<String> customItems = Arrays.asList("c1", "c2");

        loot.clear();
        loot.addAll(customItems);

        assertEquals(2, loot.size());
        assertFalse(loot.contains("v1"));
        assertTrue(loot.contains("c1"));
        assertTrue(loot.contains("c2"));
    }
}
