package com.ejyqyl.glowcmd;

import com.ejyqyl.glowcmd.util.PlaceholderUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class PlaceholderUtilTest {

    @Test
    void testConsolePlaceholders() {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        when(console.getName()).thenReturn("CONSOLE");

        String template = "Hello {PLAYER}, your username is {USERNAME} in world {WORLD}";
        String result = PlaceholderUtil.applySender(template, console);

        assertEquals("Hello CONSOLE, your username is CONSOLE in world console", result);
    }

    @Test
    void testPlayerPlaceholders() {
        Player player = Mockito.mock(Player.class);
        World world = Mockito.mock(World.class);
        Location location = new Location(world, 100.5, 64.0, -200.5);

        when(player.getName()).thenReturn("ejyqyl");
        when(player.getDisplayName()).thenReturn("§b[Dev] ejyqyl");
        when(player.getLocation()).thenReturn(location);
        when(world.getName()).thenReturn("world");

        String template = "{PLAYER} ({DISPLAYNAME}) at {WORLD}:{X},{Y},{Z}";
        String result = PlaceholderUtil.applySender(template, player);

        assertEquals("ejyqyl (§b[Dev] ejyqyl) at world:100,64,-201", result);
    }

    @Test
    void testCustomMapPlaceholders() {
        Map<String, String> map = new HashMap<>();
        map.put("{prefix}", "[GlowCMD]");
        map.put("{TARGET}", "Notch");

        String template = "{prefix} Teleported {TARGET} to spawn.";
        String result = PlaceholderUtil.applyMap(template, map);

        assertEquals("[GlowCMD] Teleported Notch to spawn.", result);
    }

    @Test
    void testMissingAndNullPlaceholders() {
        assertEquals("", PlaceholderUtil.applySender(null, null));
        assertEquals("", PlaceholderUtil.applySender("", null));

        String untouched = "No placeholders here!";
        assertEquals(untouched, PlaceholderUtil.applySender(untouched, null));
        assertEquals(untouched, PlaceholderUtil.applyMap(untouched, null));
    }
}
