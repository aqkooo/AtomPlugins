package com.ejyqyl.glowchatgame;

import com.ejyqyl.glowchatgame.data.PlayerStats;
import com.ejyqyl.glowchatgame.data.StatsManager;
import com.ejyqyl.glowchatgame.game.GameManager;
import com.ejyqyl.glowchatgame.hook.ChatGameExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ChatGameExpansionTest {

    private GlowChatGame plugin;
    private GameManager gameManager;
    private StatsManager statsManager;

    @BeforeEach
    void setUp() {
        plugin = Mockito.mock(GlowChatGame.class);
        PluginDescriptionFile desc = Mockito.mock(PluginDescriptionFile.class);
        when(desc.getVersion()).thenReturn("1.0.0");
        when(plugin.getDescription()).thenReturn(desc);

        gameManager = Mockito.mock(GameManager.class);
        statsManager = Mockito.mock(StatsManager.class);

        when(plugin.getGameManager()).thenReturn(gameManager);
        when(plugin.getStatsManager()).thenReturn(statsManager);
    }

    @Test
    void testExpansionMetadata() {
        ChatGameExpansion expansion = new ChatGameExpansion(plugin, "glowchatgame");
        assertEquals("glowchatgame", expansion.getIdentifier());
        assertEquals("ejyqyl, Glowdevv", expansion.getAuthor());
        assertEquals("1.0.0", expansion.getVersion());
        assertTrue(expansion.persist());
        assertTrue(expansion.canRegister());
    }

    @Test
    void testExpansionPlaceholders() {
        ChatGameExpansion expansion = new ChatGameExpansion(plugin, "glowchatgame");

        when(gameManager.isGameRunning()).thenReturn(true);
        when(gameManager.getLastWinner()).thenReturn("Steve");
        when(gameManager.getLastAnswer()).thenReturn("42");

        assertEquals("active", expansion.onRequest(null, "status"));
        assertEquals("Steve", expansion.onRequest(null, "last_winner"));
        assertEquals("42", expansion.onRequest(null, "answer"));

        UUID uuid = UUID.randomUUID();
        Player player = Mockito.mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        PlayerStats stats = new PlayerStats(uuid, "Steve", 10, 20, 10, 0L);
        when(statsManager.getCachedStats(uuid)).thenReturn(stats);

        assertEquals("10", expansion.onRequest(player, "wins"));
        assertEquals("20", expansion.onRequest(player, "games"));
        assertEquals("50.0", expansion.onRequest(player, "accuracy"));
        assertEquals("10", expansion.onRequest(player, "rewards"));
    }
}
