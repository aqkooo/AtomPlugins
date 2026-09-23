package glowseller;

import glowseller.models.PlayerData;
import glowseller.models.item.Item;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AutoSellAndMenusTest {

    @Test
    void testPlayerDataAutoSellFilterAndMode() {
        UUID uuid = UUID.randomUUID();
        PlayerData data = new PlayerData(uuid);

        // Default mode is OFF
        assertEquals(PlayerData.AutoSellMode.OFF, data.getAutoSellMode());
        assertFalse(data.isAutoSellActive());

        // Cycle through modes
        data.setAutoSellMode(PlayerData.AutoSellMode.ENABLED);
        assertTrue(data.isAutoSellActive());
        assertEquals(PlayerData.AutoSellMode.ENABLED, data.getAutoSellMode());

        data.setAutoSellMode(PlayerData.AutoSellMode.ENABLED_SESSION);
        assertTrue(data.isAutoSellActive());
        assertEquals(PlayerData.AutoSellMode.ENABLED_SESSION, data.getAutoSellMode());

        data.setAutoSellMode(PlayerData.AutoSellMode.OFF);
        assertFalse(data.isAutoSellActive());

        // Toggle filter items
        assertFalse(data.isItemAutoSellEnabled("DIAMOND"));
        data.toggleItemAutoSell("DIAMOND");
        assertTrue(data.isItemAutoSellEnabled("DIAMOND"));
        assertTrue(data.isItemAutoSellEnabled("diamond")); // case insensitive

        data.toggleItemAutoSell("DIAMOND");
        assertFalse(data.isItemAutoSellEnabled("DIAMOND"));

        // Batch set filter
        data.setAutoSellFilter(Arrays.asList("COAL", "IRON_INGOT", "GOLD_INGOT"));
        assertEquals(3, data.getAutoSellFilter().size());
        assertTrue(data.isItemAutoSellEnabled("COAL"));
        assertTrue(data.isItemAutoSellEnabled("IRON_INGOT"));
        assertTrue(data.isItemAutoSellEnabled("GOLD_INGOT"));
        assertFalse(data.isItemAutoSellEnabled("DIAMOND"));

        // Setting item explicitly
        data.setItemAutoSell("DIAMOND", true);
        assertTrue(data.isItemAutoSellEnabled("DIAMOND"));
        data.setItemAutoSell("DIAMOND", false);
        assertFalse(data.isItemAutoSellEnabled("DIAMOND"));
    }

    @Test
    void testItemCategorySupport() {
        Item mineItem = new Item("COAL", Material.COAL, null, 10.0, 2L, "Уголь", "mine");
        assertEquals("mine", mineItem.getCategory());
        assertEquals(Material.COAL, mineItem.getMaterial());
        assertEquals(10.0, mineItem.getPrice());
        assertEquals(2L, mineItem.getPoints());

        Item blockItem = new Item("OAK_LOG", Material.OAK_LOG, null, 117.0, 30L, "Дубовое бревно", "blocks");
        assertEquals("blocks", blockItem.getCategory());

        Item plantItem = new Item("CARROT", Material.CARROT, null, 6.0, 2L, "Морковь", "plants");
        assertEquals("plants", plantItem.getCategory());

        Item mobItem = new Item("COOKED_BEEF", Material.COOKED_BEEF, null, 11.0, 3L, "Стейк", "mobs");
        assertEquals("mobs", mobItem.getCategory());

        Item miscItem = new Item("POTION", Material.POTION, null, 482.0, 97L, "Зелье", "misc");
        assertEquals("misc", miscItem.getCategory());
    }

    @Test
    void testSellMenuInteractivityAndDupeSafety() {
        glowseller.Main mockPlugin = org.mockito.Mockito.mock(glowseller.Main.class);
        org.bukkit.entity.Player mockPlayer = org.mockito.Mockito.mock(org.bukkit.entity.Player.class);
        org.bukkit.inventory.PlayerInventory mockPlayerInv = org.mockito.Mockito.mock(org.bukkit.inventory.PlayerInventory.class);
        org.mockito.Mockito.when(mockPlayer.getInventory()).thenReturn(mockPlayerInv);

        glowseller.menu.SellMenu menu = new glowseller.menu.SellMenu(mockPlugin, mockPlayer);
        assertTrue(menu.isInteractive());

        // Slots 0-44 must be interactive drop slots
        for (int i = 0; i <= 44; i++) {
            assertTrue(menu.isSlotInteractive(i), "Slot " + i + " should be interactive");
        }

        // Control slots 45-53 must NEVER be interactive
        for (int i = 45; i <= 53; i++) {
            assertFalse(menu.isSlotInteractive(i), "Slot " + i + " must NOT be interactive");
        }

        // Shift click on top inventory (e.g. on a control slot) must be DENIED and not cloned into drop slots
        org.bukkit.event.inventory.InventoryClickEvent mockEvent = org.mockito.Mockito.mock(org.bukkit.event.inventory.InventoryClickEvent.class);
        org.bukkit.inventory.Inventory mockTopInv = org.mockito.Mockito.mock(org.bukkit.inventory.Inventory.class);
        org.mockito.Mockito.when(mockEvent.isShiftClick()).thenReturn(true);
        org.mockito.Mockito.when(mockEvent.getClickedInventory()).thenReturn(mockTopInv); // NOT player inv!

        menu.handleClick(mockEvent);

        org.mockito.Mockito.verify(mockEvent).setCancelled(true);
        org.mockito.Mockito.verify(mockEvent).setResult(org.bukkit.event.Event.Result.DENY);
        org.mockito.Mockito.verify(mockPlayer).updateInventory();
    }
}
