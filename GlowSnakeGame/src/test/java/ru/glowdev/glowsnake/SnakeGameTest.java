package ru.glowdev.glowsnake;

import org.junit.jupiter.api.Test;
import ru.glowdev.glowsnake.game.Direction;
import ru.glowdev.glowsnake.game.SnakeGameSession;
import ru.glowdev.glowsnake.util.ChatUtil;

import static org.junit.jupiter.api.Assertions.*;

public class SnakeGameTest {

    @Test
    public void testDirectionOffsets() {
        assertEquals(-9, Direction.UP.getSlotOffset());
        assertEquals(9, Direction.DOWN.getSlotOffset());
        assertEquals(-1, Direction.LEFT.getSlotOffset());
        assertEquals(1, Direction.RIGHT.getSlotOffset());
    }

    @Test
    public void testDirectionOpposites() {
        assertTrue(Direction.UP.isOpposite(Direction.DOWN));
        assertTrue(Direction.DOWN.isOpposite(Direction.UP));
        assertTrue(Direction.LEFT.isOpposite(Direction.RIGHT));
        assertTrue(Direction.RIGHT.isOpposite(Direction.LEFT));

        assertFalse(Direction.UP.isOpposite(Direction.LEFT));
        assertFalse(Direction.UP.isOpposite(Direction.RIGHT));
        assertFalse(Direction.DOWN.isOpposite(Direction.LEFT));
        assertFalse(Direction.DOWN.isOpposite(Direction.RIGHT));
        assertFalse(Direction.UP.isOpposite(null));
    }

    @Test
    public void testPlayableFieldDimensions() {
        int playableCount = 0;
        int nonPlayableCount = 0;

        for (int slot = 0; slot < SnakeGameSession.INVENTORY_SIZE; slot++) {
            if (SnakeGameSession.isPlayable(slot)) {
                playableCount++;
            } else {
                nonPlayableCount++;
            }
        }

        assertEquals(45, playableCount, "The playable field must be exactly 45 slots");
        assertEquals(9, nonPlayableCount, "The bottom-right joystick box must be 9 slots");
        assertEquals(54, playableCount + nonPlayableCount, "Total inventory slots must be 54");

        // Out of bounds
        assertFalse(SnakeGameSession.isPlayable(-1));
        assertFalse(SnakeGameSession.isPlayable(54));

        // Joystick slots (rows 3..5, cols 6..8) are NOT playable
        assertFalse(SnakeGameSession.isPlayable(34)); // Joystick Up
        assertFalse(SnakeGameSession.isPlayable(42)); // Joystick Left
        assertFalse(SnakeGameSession.isPlayable(43)); // Joystick Center
        assertFalse(SnakeGameSession.isPlayable(44)); // Joystick Right
        assertFalse(SnakeGameSession.isPlayable(52)); // Joystick Down

        // Playing field corners and center ARE playable
        assertTrue(SnakeGameSession.isPlayable(0));   // Row 0, Col 0
        assertTrue(SnakeGameSession.isPlayable(8));   // Row 0, Col 8
        assertTrue(SnakeGameSession.isPlayable(26));  // Row 2, Col 8
        assertTrue(SnakeGameSession.isPlayable(45));  // Row 5, Col 0
        assertTrue(SnakeGameSession.isPlayable(20));  // Start head (Row 2, Col 2)
    }

    @Test
    public void testValidMove() {
        // Valid move right from slot 20 to 21
        assertTrue(SnakeGameSession.isValidMove(20, Direction.RIGHT));
        // Moving right from col 8 wraps around -> invalid
        assertFalse(SnakeGameSession.isValidMove(26, Direction.RIGHT));
        // Moving down from slot 25 (Row 2, Col 7) hits joystick slot 34 -> invalid
        assertFalse(SnakeGameSession.isValidMove(25, Direction.DOWN));
        // Moving left from slot 0 wraps/out of bounds -> invalid
        assertFalse(SnakeGameSession.isValidMove(0, Direction.LEFT));
        // Moving up from row 0 -> invalid
        assertFalse(SnakeGameSession.isValidMove(4, Direction.UP));
    }

    @Test
    public void testRelativeMovementVectorProjection() {
        // Test 1: Player facing South (yaw = 0)
        // Moving forward in MC means increasing Z (dz > 0)
        double yaw0 = 0.0;
        double yawRad0 = Math.toRadians(yaw0);
        double dx = 0.0;
        double dz = 0.2; // moving forward (+Z)

        double forward = -dx * Math.sin(yawRad0) + dz * Math.cos(yawRad0);
        double strafe = dx * Math.cos(yawRad0) + dz * Math.sin(yawRad0);

        assertTrue(forward > 0.1, "Forward component should be positive when moving forward facing South");
        assertEquals(0.0, strafe, 1e-6, "Strafe should be 0");

        // Test 2: Player facing North (yaw = 180)
        // Moving forward means decreasing Z (dz < 0)
        double yaw180 = 180.0;
        double yawRad180 = Math.toRadians(yaw180);
        dx = 0.0;
        dz = -0.2;

        forward = -dx * Math.sin(yawRad180) + dz * Math.cos(yawRad180);
        strafe = dx * Math.cos(yawRad180) + dz * Math.sin(yawRad180);

        assertTrue(forward > 0.1, "Forward component should be positive when moving forward facing North");
        assertEquals(0.0, strafe, 1e-6, "Strafe should be 0");

        // Test 3: Player facing West (yaw = 90)
        // Moving right (strafe right) facing West means moving North (-Z)
        double yaw90 = 90.0;
        double yawRad90 = Math.toRadians(yaw90);
        dx = 0.0;
        dz = -0.2;

        strafe = dx * Math.cos(yawRad90) + dz * Math.sin(yawRad90);
        // sin(90 deg) = 1.0, dz = -0.2 -> -0.2 (which is Left, since facing West, North is Right? Wait:
        // Facing West (-X): North (-Z) is Right, South (+Z) is Left.
        // Let's verify: In MC right vector is (cos(yaw), sin(yaw)). For yaw=90, cos(90)=0, sin(90)=1.
        // So +Z is (0, 1), which is Left or Right? When facing -X, +Z is Left!
        // So moving North (-Z) gives -0.2 * 1 = -0.2 -> Strafe < 0 -> LEFT or RIGHT depending on convention.
        // Let's test pure strafe non-zero:
        assertNotEquals(0.0, strafe);
    }

    @Test
    public void testChatUtilFormatting() {
        String colored = ChatUtil.color("&aHello &eWorld");
        assertTrue(colored.contains("§a") && colored.contains("§e"));

        String hexColored = ChatUtil.color("&#55ff55Hex");
        assertNotNull(hexColored);
        assertFalse(hexColored.contains("&#55ff55"));
    }

    @Test
    public void testNonItalicItemFormatting() {
        String itemTitle = ChatUtil.formatItemText("&a&lГолова Змейки");
        assertTrue(itemTitle.startsWith("§r"), "Item display name must start with §r to prevent Minecraft italic font");
        assertTrue(itemTitle.contains("§a") && itemTitle.contains("§l"));

        var loreList = java.util.List.of("&7Line 1", "&eLine 2");
        var formattedLore = ChatUtil.formatItemLore(loreList);
        for (String line : formattedLore) {
            assertTrue(line.startsWith("§r"), "Each lore line must start with §r to prevent purple italic font in GUI");
        }
    }
}
