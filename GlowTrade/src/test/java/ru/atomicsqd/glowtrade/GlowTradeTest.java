package ru.atomicsqd.glowtrade;

import org.junit.jupiter.api.Test;
import ru.atomicsqd.glowtrade.gui.TradeGUI;
import ru.atomicsqd.glowtrade.util.ColorUtil;
import ru.atomicsqd.glowtrade.util.SoundUtil;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class GlowTradeTest {

    @Test
    public void testGradientParsing() {
        String input = "<gradient:#FFA500:#FFD700>GlowTrade >> Безопасный обмен</gradient>";
        String result = ColorUtil.colorize(input);

        assertNotNull(result);
        assertFalse(result.contains("<gradient:"));
        assertFalse(result.contains("</gradient>"));
        assertTrue(net.md_5.bungee.api.ChatColor.stripColor(result).contains("GlowTrade"));
    }

    @Test
    public void testNestedGradientParsing() {
        String nested = "<gradient:#FFA500:#FFD700>Нажмите /trade accept mvxqol <gradient:#FFA500:#FFD700>чтобы согласиться (30 сек).</gradient>";
        String res = ColorUtil.colorize(nested);

        assertNotNull(res);
        assertFalse(res.contains("<gradient:"));
        assertFalse(res.contains("</gradient>"));
        assertTrue(net.md_5.bungee.api.ChatColor.stripColor(res).contains("Нажмите /trade accept mvxqol чтобы согласиться (30 сек)."));
    }

    @Test
    public void testUnclosedGradientParsing() {
        String unclosed = "<gradient:#FFA500:#FFD700>Текст без закрывающего тега";
        String res = ColorUtil.colorize(unclosed);

        assertNotNull(res);
        assertFalse(res.contains("<gradient:"));
        assertTrue(net.md_5.bungee.api.ChatColor.stripColor(res).contains("Текст без закрывающего тега"));
    }

    @Test
    public void testClosingTagStripping() {
        String input = "<#FF5555>Обмен отменен!</#FF5555>";
        String result = ColorUtil.colorize(input);

        assertNotNull(result);
        assertFalse(result.contains("</#FF5555>"));
        assertFalse(result.contains("</"));
        assertTrue(net.md_5.bungee.api.ChatColor.stripColor(result).contains("Обмен отменен!"));
    }

    @Test
    public void testRedAndGreenGradients() {
        String red = ColorUtil.redGradient("НЕ ГОТОВ");
        assertNotNull(red);
        assertTrue(net.md_5.bungee.api.ChatColor.stripColor(red).contains("НЕ ГОТОВ"));

        String green = ColorUtil.greenGradient("ГОТОВ");
        assertNotNull(green);
        assertTrue(net.md_5.bungee.api.ChatColor.stripColor(green).contains("ГОТОВ"));
    }

    @Test
    public void testGuiItemAntiAnvilItalic() {
        String displayName = "Готов";
        String formatted = ColorUtil.guiItemName(displayName);

        // Должно начинаться с §r для сброса ванильного курсива наковальни
        assertTrue(formatted.startsWith("§r"));
    }

    @Test
    public void test54SlotPartitioning() {
        // Проверяем идеальное разделение слотов двойного сундука (54 слота, 0..53)
        Set<Integer> allSlots = new HashSet<>();

        // Слоты игрока 1 (16)
        assertEquals(16, TradeGUI.PLAYER_1_OFFER_SLOTS.size());
        for (int slot : TradeGUI.PLAYER_1_OFFER_SLOTS) {
            assertTrue(allSlots.add(slot), "Duplicate slot found: " + slot);
        }

        // Слоты игрока 2 (16)
        assertEquals(16, TradeGUI.PLAYER_2_OFFER_SLOTS.size());
        for (int slot : TradeGUI.PLAYER_2_OFFER_SLOTS) {
            assertTrue(allSlots.add(slot), "Duplicate slot found: " + slot);
        }

        // Разделители (5)
        assertEquals(5, TradeGUI.SEPARATOR_SLOTS.size());
        for (int slot : TradeGUI.SEPARATOR_SLOTS) {
            assertTrue(allSlots.add(slot), "Duplicate slot found: " + slot);
        }

        // Кнопки игрока 1 (4)
        assertEquals(4, TradeGUI.PLAYER_1_READY_SLOTS.size());
        for (int slot : TradeGUI.PLAYER_1_READY_SLOTS) {
            assertTrue(allSlots.add(slot), "Duplicate slot found: " + slot);
        }

        // Кнопки игрока 2 (4)
        assertEquals(4, TradeGUI.PLAYER_2_READY_SLOTS.size());
        for (int slot : TradeGUI.PLAYER_2_READY_SLOTS) {
            assertTrue(allSlots.add(slot), "Duplicate slot found: " + slot);
        }

        // Слот таймера (1)
        assertTrue(allSlots.add(TradeGUI.TIMER_SLOT), "Duplicate slot found: " + TradeGUI.TIMER_SLOT);

        // Заполнители (8)
        assertEquals(8, TradeGUI.FILLER_SLOTS.size());
        for (int slot : TradeGUI.FILLER_SLOTS) {
            assertTrue(allSlots.add(slot), "Duplicate slot found: " + slot);
        }

        // Проверяем, что покрыты ровно все 54 слота от 0 до 53
        assertEquals(54, allSlots.size());
        for (int i = 0; i < 54; i++) {
            assertTrue(allSlots.contains(i), "Slot " + i + " is missing from layout!");
        }
    }

    @Test
    public void testSoundFallbackSafety() {
        // Проверяем, что резолвер звуков не бросает исключений при неизвестных названиях
        assertDoesNotThrow(() -> SoundUtil.resolveSound("NON_EXISTENT_SOUND_NAME"));
        assertDoesNotThrow(() -> SoundUtil.resolveSound("ENTITY_EXPERIENCE_ORB_PICKUP"));
        assertDoesNotThrow(() -> SoundUtil.resolveSound("BLOCK_NOTE_BLOCK_PLING"));
    }
}
