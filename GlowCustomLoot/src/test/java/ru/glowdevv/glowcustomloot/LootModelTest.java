package ru.glowdevv.glowcustomloot;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.glowdevv.glowcustomloot.model.DimensionType;
import ru.glowdevv.glowcustomloot.model.LootItem;
import ru.glowdevv.glowcustomloot.model.LootMode;
import ru.glowdevv.glowcustomloot.model.StructureLootTable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class LootModelTest {

    @Test
    void testDimensionTypeParsing() {
        assertEquals(DimensionType.OVERWORLD, DimensionType.fromString("overworld"));
        assertEquals(DimensionType.OVERWORLD, DimensionType.fromString("world"));
        assertEquals(DimensionType.NETHER, DimensionType.fromString("nether"));
        assertEquals(DimensionType.NETHER, DimensionType.fromString("the_nether"));
        assertEquals(DimensionType.THE_END, DimensionType.fromString("end"));
        assertEquals(DimensionType.THE_END, DimensionType.fromString("the_end"));
        assertNull(DimensionType.fromString("unknown_dimension"));
    }

    @Test
    void testLootModeParsing() {
        assertEquals(LootMode.MERGE, LootMode.fromString("MERGE"));
        assertEquals(LootMode.MERGE, LootMode.fromString("merge"));
        assertEquals(LootMode.REPLACE, LootMode.fromString("REPLACE"));
        assertEquals(LootMode.REPLACE, LootMode.fromString("replace"));
        assertEquals(LootMode.MERGE, LootMode.fromString("invalid_mode"));
    }

    @Test
    void testStructureLootTableRollBounds() {
        StructureLootTable table = new StructureLootTable("test_id", "Test Table", Material.CHEST,
                DimensionType.OVERWORLD, "minecraft:chests/test", LootMode.MERGE, 2, 5, null);

        assertEquals(2, table.getMinRolls());
        assertEquals(5, table.getMaxRolls());
        assertFalse(table.isDirty());

        // Update rolls
        table.setMinRolls(6);
        assertEquals(6, table.getMinRolls());
        assertEquals(6, table.getMaxRolls(), "Max rolls should adjust if lower than min");
        assertTrue(table.isDirty());

        table.setDirty(false);
        table.setMaxRolls(10);
        assertEquals(10, table.getMaxRolls());
        assertTrue(table.isDirty());
    }

    @Test
    void testLootItemChanceBounds() {
        ItemStack item = Mockito.mock(ItemStack.class);
        when(item.clone()).thenReturn(item);

        LootItem lootItem = new LootItem("1", "VANILLA", 50.0, 1, 3, item);

        assertEquals(50.0, lootItem.getChance());
        assertEquals(1, lootItem.getMinAmount());
        assertEquals(3, lootItem.getMaxAmount());

        // Test +10%
        lootItem.addChance(10.0);
        assertEquals(60.0, lootItem.getChance());

        // Test clamped to 100%
        lootItem.addChance(100.0);
        assertEquals(100.0, lootItem.getChance());

        // Test clamped to 0.1%
        lootItem.addChance(-200.0);
        assertEquals(0.1, lootItem.getChance());
    }

    @Test
    void testStructureLootTableItemManagement() {
        StructureLootTable table = new StructureLootTable("desert_pyramid", "Пустынная пирамида",
                Material.CHEST, DimensionType.OVERWORLD, "minecraft:chests/desert_pyramid", LootMode.MERGE, 1, 3, null);

        ItemStack item = Mockito.mock(ItemStack.class);
        when(item.clone()).thenReturn(item);

        LootItem item1 = new LootItem("1", "VANILLA", 30.0, 1, 2, item);
        LootItem item2 = new LootItem("2", "VANILLA", 75.0, 2, 5, item);

        table.addItem(item1);
        table.addItem(item2);

        assertEquals(2, table.getItems().size());
        assertEquals("3", table.nextAvailableId());
        assertTrue(table.isDirty());

        assertTrue(table.removeItem("1"));
        assertEquals(1, table.getItems().size());
        assertNull(table.getItem("1"));
        assertNotNull(table.getItem("2"));
    }

    @Test
    void testMultipleLootTableKeys() {
        java.util.List<String> keys = java.util.Arrays.asList("minecraft:chests/ancient_city", "minecraft:chests/ancient_city_ice_box");
        StructureLootTable table = new StructureLootTable("ancient_city", "Древний город",
                Material.CHEST, DimensionType.OVERWORLD, keys, LootMode.MERGE, 2, 4, null);

        assertEquals(2, table.getLootTableKeys().size());
        assertEquals("minecraft:chests/ancient_city", table.getLootTableKey());
        assertTrue(table.getLootTableKeys().contains("minecraft:chests/ancient_city_ice_box"));
    }

    @Test
    void testRegistryMaterialsValid() {
        String[] materials = {
                "WHEAT_SEEDS", "BEETROOT_SEEDS", "PUMPKIN_SEEDS", "MELON_SEEDS",
                "TORCHFLOWER_SEEDS", "PITCHER_POD", "WHEAT", "HAY_BLOCK", "CARROT",
                "POTATO", "POISONOUS_POTATO", "BEETROOT", "SWEET_BERRIES", "GLOW_BERRIES",
                "APPLE", "GOLDEN_APPLE", "ENCHANTED_GOLDEN_APPLE", "MELON_SLICE", "PUMPKIN",
                "BAMBOO", "CACTUS", "SUGAR_CANE", "NETHER_WART", "CHORUS_FRUIT", "MOSS_BLOCK",
                "COAL", "CHARCOAL", "RAW_IRON", "IRON_INGOT", "IRON_NUGGET", "RAW_GOLD",
                "GOLD_INGOT", "GOLD_NUGGET", "RAW_COPPER", "COPPER_INGOT", "LAPIS_LAZULI",
                "REDSTONE", "DIAMOND", "EMERALD", "AMETHYST_SHARD", "QUARTZ",
                "NETHERITE_SCRAP", "NETHERITE_INGOT", "BOW", "CROSSBOW", "ARROW", "SPECTRAL_ARROW",
                "HEAVY_CORE", "BREEZE_ROD", "SHIELD", "FISHING_ROD", "FLINT_AND_STEEL",
                "COMPASS", "RECOVERY_COMPASS", "CLOCK", "SPYGLASS", "CHAINMAIL_HELMET",
                "CHAINMAIL_CHESTPLATE", "CHAINMAIL_LEGGINGS", "CHAINMAIL_BOOTS",
                "NETHERITE_UPGRADE_SMITHING_TEMPLATE", "SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE",
                "VEX_ARMOR_TRIM_SMITHING_TEMPLATE", "WILD_ARMOR_TRIM_SMITHING_TEMPLATE",
                "COAST_ARMOR_TRIM_SMITHING_TEMPLATE", "DUNE_ARMOR_TRIM_SMITHING_TEMPLATE",
                "WARD_ARMOR_TRIM_SMITHING_TEMPLATE", "SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE",
                "SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE", "RIB_ARMOR_TRIM_SMITHING_TEMPLATE",
                "SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE", "FLOW_ARMOR_TRIM_SMITHING_TEMPLATE",
                "BOLT_ARMOR_TRIM_SMITHING_TEMPLATE", "SADDLE", "NAME_TAG", "LEAD",
                "IRON_HORSE_ARMOR", "GOLDEN_HORSE_ARMOR", "DIAMOND_HORSE_ARMOR",
                "TOTEM_OF_UNDYING", "ELYTRA", "HEART_OF_THE_SEA", "ECHO_SHARD",
                "DISC_FRAGMENT_5", "TRIAL_KEY", "OMINOUS_TRIAL_KEY", "OMINOUS_BOTTLE",
                "DRAGON_HEAD", "WITHER_SKELETON_SKULL", "MUSIC_DISC_OTHERSIDE",
                "MUSIC_DISC_PIGSTEP", "MUSIC_DISC_5", "MUSIC_DISC_CREATOR",
                "MUSIC_DISC_PRECIPICE", "SCULK_CATALYST", "SCULK_SENSOR", "SCULK_SHRIEKER",
                "ANGLER_POTTERY_SHERD", "ARCHER_POTTERY_SHERD", "FLOW_POTTERY_SHERD",
                "GUSTER_POTTERY_SHERD", "SCRAPE_POTTERY_SHERD", "SHELTER_POTTERY_SHERD",
                "SKULL_POTTERY_SHERD", "PRIZE_POTTERY_SHERD", "MINER_POTTERY_SHERD",
                "ENCHANTED_BOOK"
        };
        for (String matName : materials) {
            Material mat = Material.matchMaterial(matName);
            assertNotNull(mat, "Material should exist in Paper 1.21 API: " + matName);
        }
    }

    @Test
    void testGenerateLootRollBoundsAndCapping() {
        StructureLootTable table = new StructureLootTable("simple_dungeon", "Сокровищница",
                Material.SPAWNER, DimensionType.OVERWORLD, "minecraft:chests/simple_dungeon", LootMode.MERGE, 2, 4, null);

        ItemStack item = Mockito.mock(ItemStack.class);
        when(item.clone()).thenReturn(item);
        when(item.getType()).thenReturn(Material.DIAMOND);
        when(item.getMaxStackSize()).thenReturn(64);
        when(item.getAmount()).thenReturn(1);

        // Add 5 items with 100% chance
        for (int i = 1; i <= 5; i++) {
            table.addItem(new LootItem(String.valueOf(i), "VANILLA", 100.0, 1, 1, item));
        }

        java.util.Random rnd = new java.util.Random(12345);
        List<ItemStack> rolled = table.generateLoot(rnd, 27);

        assertFalse(rolled.isEmpty());
        assertTrue(rolled.size() >= 2 && rolled.size() <= 4,
                "Rolled items count (" + rolled.size() + ") should be within minRolls(2) and maxRolls(4)");

        // Test with low cap
        List<ItemStack> capped = table.generateLoot(rnd, 1);
        assertEquals(1, capped.size(), "Should cap at maxItemsCap=1");
    }
}

