package glowseller;

import glowseller.models.PlayerData;
import glowseller.models.ShopItem;
import glowseller.models.item.Item;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ShopItemAndItemMatchingTest {

    @Test
    @DisplayName("Verify Item matching with Material and CustomModelData")
    void testItemMatching() {
        Item plainDiamond = new Item("DIAMOND", Material.DIAMOND, null, 50.0, 2L, "Алмаз");
        assertEquals("DIAMOND", plainDiamond.getId());
        assertEquals(Material.DIAMOND, plainDiamond.getMaterial());
        assertEquals(50.0, plainDiamond.getPrice());
        assertEquals(2L, plainDiamond.getPoints());

        // ItemStack with diamond
        ItemStack stack = Mockito.mock(ItemStack.class);
        Mockito.when(stack.getType()).thenReturn(Material.DIAMOND);

        assertTrue(plainDiamond.matches(stack));

        // Different material
        ItemStack goldStack = Mockito.mock(ItemStack.class);
        Mockito.when(goldStack.getType()).thenReturn(Material.GOLD_INGOT);
        assertFalse(plainDiamond.matches(goldStack));

        // Item with CustomModelData
        Item customDiamond = new Item("CUSTOM_DIAMOND", Material.DIAMOND, 1234, 100.0, 5L, "Кастомный Алмаз");
        ItemMeta meta = Mockito.mock(ItemMeta.class);
        Mockito.when(stack.getItemMeta()).thenReturn(meta);
        Mockito.when(meta.hasCustomModelData()).thenReturn(true);
        Mockito.when(meta.getCustomModelData()).thenReturn(1234);

        assertTrue(customDiamond.matches(stack));

        // Wrong model data
        Mockito.when(meta.getCustomModelData()).thenReturn(9999);
        assertFalse(customDiamond.matches(stack));
    }

    @Test
    @DisplayName("Verify ShopItem model properties")
    void testShopItemProperties() {
        ShopItem booster = new ShopItem("coin_x2_1h", "boosters");
        booster.setType(ShopItem.Type.BOOSTER);
        booster.setMultiplier(2.0);
        booster.setDurationSeconds(3600);
        booster.setPrice(500);

        assertEquals("coin_x2_1h", booster.getKey());
        assertEquals("boosters", booster.getCategory());
        assertEquals(ShopItem.Type.BOOSTER, booster.getType());
        assertEquals(2.0, booster.getMultiplier());
        assertEquals(3600, booster.getDurationSeconds());
        assertEquals(500, booster.getPrice());
    }

    @Test
    @DisplayName("Verify PlayerData points operations")
    void testPlayerDataPoints() {
        PlayerData data = new PlayerData(UUID.randomUUID());
        assertEquals(0L, data.getPoints());

        data.addPoints(100L);
        assertEquals(100L, data.getPoints());

        // Negative check on take
        assertFalse(data.takePoints(150L));
        assertEquals(100L, data.getPoints()); // not deducted

        assertTrue(data.takePoints(50L));
        assertEquals(50L, data.getPoints());

        assertTrue(data.takePoints(50L));
        assertEquals(0L, data.getPoints());
    }
}
