package com.ejyqyl.glowcmd;

import com.ejyqyl.glowcmd.command.GlowCMDCommand;
import com.ejyqyl.glowcmd.command.SetFirstSpawnCommand;
import com.ejyqyl.glowcmd.command.SetSpawnCommand;
import com.ejyqyl.glowcmd.command.SpawnCommand;
import com.ejyqyl.glowcmd.config.ConfigManager;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.listener.PlayerJoinListener;
import com.ejyqyl.glowcmd.listener.PlayerRespawnListener;
import com.ejyqyl.glowcmd.model.SpawnPoint;
import com.ejyqyl.glowcmd.spawn.SpawnManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CommandAndListenerTest {

    private GlowCMD plugin;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private SpawnManager spawnManager;
    private Command dummyCommand;

    @BeforeEach
    void setUp() {
        plugin = Mockito.mock(GlowCMD.class);
        configManager = Mockito.mock(ConfigManager.class);
        messageManager = Mockito.mock(MessageManager.class);
        spawnManager = Mockito.mock(SpawnManager.class);

        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getMessageManager()).thenReturn(messageManager);
        when(plugin.getSpawnManager()).thenReturn(spawnManager);

        PluginDescriptionFile description = Mockito.mock(PluginDescriptionFile.class);
        when(description.getVersion()).thenReturn("1.0.0");
        when(plugin.getDescription()).thenReturn(description);

        dummyCommand = Mockito.mock(Command.class);
    }

    @Test
    void testSpawnCommandConsoleSender() {
        SpawnCommand cmd = new SpawnCommand(plugin);
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);

        boolean result = cmd.onCommand(console, dummyCommand, "spawn", new String[0]);
        assertTrue(result);
        verify(messageManager).send(console, "player-only");
    }

    @Test
    void testSpawnCommandNoPermission() {
        SpawnCommand cmd = new SpawnCommand(plugin);
        Player player = Mockito.mock(Player.class);
        when(player.hasPermission("glowcmd.spawn")).thenReturn(false);

        boolean result = cmd.onCommand(player, dummyCommand, "spawn", new String[0]);
        assertTrue(result);
        verify(messageManager).send(player, "no-permission");
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    void testSpawnCommandNotSet() {
        SpawnCommand cmd = new SpawnCommand(plugin);
        Player player = Mockito.mock(Player.class);
        when(player.hasPermission("glowcmd.spawn")).thenReturn(true);
        when(configManager.isSpawnEnabled()).thenReturn(true);
        when(spawnManager.getSpawn()).thenReturn(null);

        boolean result = cmd.onCommand(player, dummyCommand, "spawn", new String[0]);
        assertTrue(result);
        verify(messageManager).send(player, "spawn.not-set");
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    void testSetSpawnCommand() {
        SetSpawnCommand cmd = new SetSpawnCommand(plugin);
        Player player = Mockito.mock(Player.class);
        World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("world");
        Location loc = new Location(world, 10.0, 64.0, 10.0, 0.0f, 0.0f);

        when(player.hasPermission("glowcmd.setspawn")).thenReturn(true);
        when(player.getLocation()).thenReturn(loc);

        boolean result = cmd.onCommand(player, dummyCommand, "setspawn", new String[0]);
        assertTrue(result);
        verify(spawnManager).setSpawn(any(SpawnPoint.class));
        verify(messageManager).send(player, "spawn.set");
    }

    @Test
    void testSetFirstSpawnCommand() {
        SetFirstSpawnCommand cmd = new SetFirstSpawnCommand(plugin);
        Player player = Mockito.mock(Player.class);
        World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("lobby");
        Location loc = new Location(world, 50.0, 70.0, 50.0, 90.0f, 0.0f);

        when(player.hasPermission("glowcmd.setfirstspawn")).thenReturn(true);
        when(player.getLocation()).thenReturn(loc);

        boolean result = cmd.onCommand(player, dummyCommand, "setfirstspawn", new String[0]);
        assertTrue(result);
        verify(spawnManager).setFirstSpawn(any(SpawnPoint.class));
        verify(messageManager).send(player, "first-spawn.set");
    }

    @Test
    void testGlowCMDCommandReload() {
        GlowCMDCommand cmd = new GlowCMDCommand(plugin);
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        when(console.hasPermission("glowcmd.reload")).thenReturn(true);

        boolean result = cmd.onCommand(console, dummyCommand, "glowcmd", new String[]{"reload"});
        assertTrue(result);
        verify(plugin).reloadAll();
        verify(messageManager).send(console, "reload.success");
    }

    @Test
    void testGlowCMDTabComplete() {
        GlowCMDCommand cmd = new GlowCMDCommand(plugin);
        Player player = Mockito.mock(Player.class);
        when(player.hasPermission("glowcmd.reload")).thenReturn(true);

        List<String> tabs = cmd.onTabComplete(player, dummyCommand, "glowcmd", new String[]{""});
        assertNotNull(tabs);
        assertEquals(1, tabs.size());
        assertEquals("reload", tabs.get(0));

        when(player.hasPermission("glowcmd.reload")).thenReturn(false);
        List<String> noTabs = cmd.onTabComplete(player, dummyCommand, "glowcmd", new String[]{""});
        assertNotNull(noTabs);
        assertTrue(noTabs.isEmpty());
    }

    @Test
    void testPlayerJoinListenerFirstJoin() {
        PlayerJoinListener listener = new PlayerJoinListener(plugin);
        Player player = Mockito.mock(Player.class);
        when(player.getName()).thenReturn("Newbie");
        when(player.hasPlayedBefore()).thenReturn(false); // First join!

        when(configManager.isSpawnEnabled()).thenReturn(true);
        when(configManager.isFirstSpawnEnabled()).thenReturn(true);
        when(configManager.isTeleportOnFirstJoin()).thenReturn(true);

        SpawnPoint point = Mockito.mock(SpawnPoint.class);
        Location loc = Mockito.mock(Location.class);
        when(point.toLocation()).thenReturn(loc);
        when(spawnManager.getFirstSpawn()).thenReturn(point);

        PlayerJoinEvent event = new PlayerJoinEvent(player, "Newbie joined");
        listener.onPlayerJoin(event);

        verify(player).teleport(loc);
        verify(messageManager).send(player, "first-spawn.teleport");
    }

    @Test
    void testPlayerJoinListenerSubsequentJoin() {
        PlayerJoinListener listener = new PlayerJoinListener(plugin);
        Player player = Mockito.mock(Player.class);
        when(player.hasPlayedBefore()).thenReturn(true); // NOT first join!

        PlayerJoinEvent event = new PlayerJoinEvent(player, "Player joined");
        listener.onPlayerJoin(event);

        verify(player, never()).teleport(any(Location.class));
        verify(messageManager, never()).send(any(), anyString());
    }

    @Test
    void testPlayerRespawnListener() {
        PlayerRespawnListener listener = new PlayerRespawnListener(plugin);
        Player player = Mockito.mock(Player.class);

        when(configManager.isSpawnEnabled()).thenReturn(true);
        when(configManager.isTeleportOnDeath()).thenReturn(true);

        World world = Mockito.mock(World.class);
        Location spawnLoc = new Location(world, 10.0, 64.0, 10.0);
        Location initialLoc = new Location(world, 0.0, 64.0, 0.0);

        SpawnPoint spawn = Mockito.mock(SpawnPoint.class);
        when(spawn.toLocation()).thenReturn(spawnLoc);
        when(spawnManager.getSpawn()).thenReturn(spawn);

        PlayerRespawnEvent event = new PlayerRespawnEvent(player, initialLoc, false);
        listener.onPlayerRespawn(event);

        assertEquals(spawnLoc, event.getRespawnLocation());
    }
}
