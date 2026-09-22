package ru.atomicsqd.atommessage;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import ru.atomicsqd.atommessage.util.ColorUtil;

import static org.junit.jupiter.api.Assertions.*;

public class ColorUtilTest {

    @Test
    public void testMiniMessageGradient() {
        String input = "<gradient:#FF512F:#DD2476><b>СКИДКА НА ДОНАТ 20%</b></gradient>";
        Component component = ColorUtil.parseComponent(input, null);
        assertNotNull(component);
        assertEquals("СКИДКА НА ДОНАТ 20%", ColorUtil.stripColor(input));
    }

    @Test
    public void testLegacyAndHexMixed() {
        String input = "<gradient:#0088CC:#00C6FF>Telegram:</gradient> &#FDBF5F@atomicsqd &7(&eновости&7)";
        Component component = ColorUtil.parseComponent(input, null);
        assertNotNull(component);
        assertEquals("Telegram: @atomicsqd (новости)", ColorUtil.stripColor(input));

        String legacy = ColorUtil.toLegacyString(input, null);
        assertNotNull(legacy);
        assertTrue(legacy.contains("@atomicsqd"));
    }

    @Test
    public void testRainbow() {
        String input = "<rainbow>Радужный текст</rainbow>";
        Component component = ColorUtil.parseComponent(input, null);
        assertNotNull(component);
        assertEquals("Радужный текст", ColorUtil.stripColor(input));
    }

    @Test
    public void testLegacySectionCodes() {
        String input = "§aЗелёный §lжирный §eжёлтый";
        Component component = ColorUtil.parseComponent(input, null);
        assertNotNull(component);
        assertEquals("Зелёный жирный жёлтый", ColorUtil.stripColor(input));
    }

    @Test
    public void testCmiAndEssentialsHex() {
        String input = "{#FF5555}Красный {#00AAFF}Синий";
        Component component = ColorUtil.parseComponent(input, null);
        assertNotNull(component);
        assertEquals("Красный Синий", ColorUtil.stripColor(input));
    }

    @Test
    public void testAmpersandXHexFormat() {
        String input = "&x&f&f&a&a&0&0Оранжевый текст";
        Component component = ColorUtil.parseComponent(input, null);
        assertNotNull(component);
        assertEquals("Оранжевый текст", ColorUtil.stripColor(input));
    }

    @Test
    public void testInteractiveBBCodeUrl() {
        String input = "[url=https://atomicsmp.fun hover=\"Нажмите для перехода\"]Наш сайт[/url]";
        Component component = ColorUtil.parseComponent(input, null);
        assertNotNull(component);
        assertEquals("Наш сайт", ColorUtil.stripColor(input));

        String preprocessed = ColorUtil.preprocessAllFormatting(input);
        assertTrue(preprocessed.contains("<click:open_url:'https://atomicsmp.fun'>"));
        assertTrue(preprocessed.contains("<hover:show_text:'Нажмите для перехода'>"));
    }

    @Test
    public void testInteractiveBBCodeCommand() {
        String input = "[cmd=/rules hover=\"Открыть правила\"]Нажмите сюда[/cmd]";
        Component component = ColorUtil.parseComponent(input, null);
        assertNotNull(component);
        assertEquals("Нажмите сюда", ColorUtil.stripColor(input));

        String preprocessed = ColorUtil.preprocessAllFormatting(input);
        assertTrue(preprocessed.contains("<click:run_command:'/rules'>"));
    }

    @Test
    public void testInteractiveBBCodeSuggestAndCopy() {
        String input = "[suggest=/pay Notch 100]Заплатить[/suggest] или [copy=PROMO2026]Скопировать промокод[/copy]";
        Component component = ColorUtil.parseComponent(input, null);
        assertNotNull(component);
        assertEquals("Заплатить или Скопировать промокод", ColorUtil.stripColor(input));

        String preprocessed = ColorUtil.preprocessAllFormatting(input);
        assertTrue(preprocessed.contains("<click:suggest_command:'/pay Notch 100'>"));
        assertTrue(preprocessed.contains("<click:copy_to_clipboard:'PROMO2026'>"));
    }

    @Test
    public void testHoverOnlyTag() {
        String input = "[hover=\"Подсказка\"]Наведи мышку[/hover]";
        Component component = ColorUtil.parseComponent(input, null);
        assertNotNull(component);
        assertEquals("Наведи мышку", ColorUtil.stripColor(input));

        String preprocessed = ColorUtil.preprocessAllFormatting(input);
        assertTrue(preprocessed.contains("<hover:show_text:'Подсказка'>"));
    }
}
