package glowseller;

import glowseller.utils.SyntaxParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SyntaxParserTest {

    @Test
    @DisplayName("Verify SyntaxParser action tags and arguments")
    void testActionParsing() {
        SyntaxParser.Action a1 = SyntaxParser.parse("[opengui] shop");
        assertEquals(SyntaxParser.ActionType.OPEN_GUI, a1.getType());
        assertEquals("shop", a1.getArgument());

        SyntaxParser.Action a2 = SyntaxParser.parse("[open] boosters");
        assertEquals(SyntaxParser.ActionType.OPEN_GUI, a2.getType());
        assertEquals("boosters", a2.getArgument());

        SyntaxParser.Action a3 = SyntaxParser.parse("[buy] coin_x2_1h");
        assertEquals(SyntaxParser.ActionType.BUY, a3.getType());
        assertEquals("coin_x2_1h", a3.getArgument());

        SyntaxParser.Action a4 = SyntaxParser.parse("[sell_all]");
        assertEquals(SyntaxParser.ActionType.SELL_ALL, a4.getType());

        SyntaxParser.Action a5 = SyntaxParser.parse("[close]");
        assertEquals(SyntaxParser.ActionType.CLOSE, a5.getType());

        SyntaxParser.Action a6 = SyntaxParser.parse("[command] crazycrates give %player% diamond_key 1");
        assertEquals(SyntaxParser.ActionType.CONSOLE_COMMAND, a6.getType());
        assertEquals("crazycrates give %player% diamond_key 1", a6.getArgument());

        SyntaxParser.Action a7 = SyntaxParser.parse("[sound] ENTITY_PLAYER_LEVELUP");
        assertEquals(SyntaxParser.ActionType.SOUND, a7.getType());
        assertEquals("ENTITY_PLAYER_LEVELUP", a7.getArgument());

        SyntaxParser.Action a8 = SyntaxParser.parse("[message] Привет, игрок!");
        assertEquals(SyntaxParser.ActionType.MESSAGE, a8.getType());
        assertEquals("Привет, игрок!", a8.getArgument());

        // Unknown
        SyntaxParser.Action a9 = SyntaxParser.parse("random text without brackets");
        assertEquals(SyntaxParser.ActionType.UNKNOWN, a9.getType());
    }
}
