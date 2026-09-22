package com.ejyqyl.glowcmd;

import com.ejyqyl.glowcmd.hook.GlowCMDExpansion;
import com.ejyqyl.glowcmd.manager.HomeManager;
import com.ejyqyl.glowcmd.manager.PrivateMessageManager;
import com.ejyqyl.glowcmd.manager.VanishManager;
import com.ejyqyl.glowcmd.manager.WarpManager;
import com.ejyqyl.glowcmd.model.SpawnPoint;
import com.ejyqyl.glowcmd.spawn.SpawnManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class PlaceholderExpansionTest {

    private GlowCMD plugin;
    private HomeManager homeManager;
    private WarpManager warpManager;
    private VanishManager vanishManager;
    private SpawnManager spawnManager;
    private PrivateMessageManager privateMessageManager;

    @BeforeEach
    void setUp() {
        plugin = Mockito.mock(GlowCMD.class);
        PluginDescriptionFile description = Mockito.mock(PluginDescriptionFile.class);
        when(description.getVersion()).thenReturn("1.0.0");
        when(plugin.getDescription()).thenReturn(description);

        homeManager = Mockito.mock(HomeManager.class);
        warpManager = Mockito.mock(WarpManager.class);
        vanishManager = Mockito.mock(VanishManager.class);
        spawnManager = Mockito.mock(SpawnManager.class);
        privateMessageManager = Mockito.mock(PrivateMessageManager.class);

        when(plugin.getHomeManager()).thenReturn(homeManager);
        when(plugin.getWarpManager()).thenReturn(warpManager);
        when(plugin.getVanishManager()).thenReturn(vanishManager);
        when(plugin.getSpawnManager()).thenReturn(spawnManager);
        when(plugin.getPrivateMessageManager()).thenReturn(privateMessageManager);
    }

    @Test
    void testExpansionMetadata() {
        GlowCMDExpansion expansion = new GlowCMDExpansion(plugin, "glowcmd");
        assertEquals("glowcmd", expansion.getIdentifier());
        assertEquals("ejyqyl", expansion.getAuthor());
        assertEquals("1.0.0", expansion.getVersion());
        assertTrue(expansion.persist());
        assertTrue(expansion.canRegister());
    }

    @Test
    void testPlayerPlaceholders() {
        GlowCMDExpansion expansion = new GlowCMDExpansion(plugin, "glowcmd");

        UUID playerId = UUID.randomUUID();
        Player player = Mockito.mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("TestUser");
        when(player.isFlying()).thenReturn(true);
        when(player.getAllowFlight()).thenReturn(true);
        when(player.isInvulnerable()).thenReturn(false);
        when(player.getWalkSpeed()).thenReturn(0.2f);
        when(player.getFlySpeed()).thenReturn(0.1f);
        when(player.getPing()).thenReturn(42);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);

        when(vanishManager.isVanished(player)).thenReturn(true);
        when(homeManager.getHomes(playerId)).thenReturn(Map.of("home", new SpawnPoint("world", 0, 64, 0, 0, 0)));
        when(homeManager.getMaxHomes(player)).thenReturn(5);
        when(warpManager.getWarps()).thenReturn(Map.of("shop", new SpawnPoint("world", 100, 64, 100, 0, 0)));
        when(spawnManager.getSpawn()).thenReturn(new SpawnPoint("world", 0, 64, 0, 0, 0));

        assertEquals("yes", expansion.onRequest(player, "fly"));
        assertEquals("yes", expansion.onRequest(player, "is_flying"));
        assertEquals("no", expansion.onRequest(player, "god"));
        assertEquals("no", expansion.onRequest(player, "godmode"));
        assertEquals("yes", expansion.onRequest(player, "vanished"));
        assertEquals("yes", expansion.onRequest(player, "is_vanished"));
        assertEquals("1.00", expansion.onRequest(player, "speed"));
        assertEquals("1.00", expansion.onRequest(player, "walk_speed"));
        assertEquals("1.00", expansion.onRequest(player, "fly_speed"));
        assertEquals("1", expansion.onRequest(player, "homes_count"));
        assertEquals("5", expansion.onRequest(player, "homes_max"));
        assertEquals("1", expansion.onRequest(player, "warps_count"));
        assertEquals("42", expansion.onRequest(player, "ping"));
        assertEquals("survival", expansion.onRequest(player, "gamemode"));
        assertEquals("true", expansion.onRequest(player, "spawn_set"));
        assertEquals("world", expansion.onRequest(player, "spawn_world"));
    }

    @Test
    void testCompatibilityIdentifiers() {
        GlowCMDExpansion glow = new GlowCMDExpansion(plugin, "glow");
        assertEquals("glow", glow.getIdentifier());

        GlowCMDExpansion essentials = new GlowCMDExpansion(plugin, "essentials");
        assertEquals("essentials", essentials.getIdentifier());
    }
}
