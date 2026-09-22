package com.ejyqyl.glowcmd;

import com.ejyqyl.glowcmd.command.player.FeedCommand;
import com.ejyqyl.glowcmd.command.player.FlyCommand;
import com.ejyqyl.glowcmd.command.player.GamemodeCommand;
import com.ejyqyl.glowcmd.command.player.HealCommand;
import com.ejyqyl.glowcmd.config.MessageManager;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExtendedCommandsTest {

    private GlowCMD plugin;
    private MessageManager messageManager;
    private Command dummyCommand;

    @BeforeEach
    void setUp() {
        plugin = Mockito.mock(GlowCMD.class);
        messageManager = Mockito.mock(MessageManager.class);
        when(plugin.getMessageManager()).thenReturn(messageManager);
        dummyCommand = Mockito.mock(Command.class);
    }

    @Test
    void testHealCommand() {
        HealCommand cmd = new HealCommand(plugin);
        Player player = Mockito.mock(Player.class);
        when(player.hasPermission("glowcmd.heal")).thenReturn(true);

        AttributeInstance attr = Mockito.mock(AttributeInstance.class);
        when(attr.getValue()).thenReturn(20.0);
        when(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).thenReturn(attr);

        boolean res = cmd.onCommand(player, dummyCommand, "heal", new String[0]);
        assertTrue(res);
        verify(player).setHealth(20.0);
        verify(player).setFoodLevel(20);
        verify(player).setFireTicks(0);
        verify(messageManager).send(player, "heal.self");
    }

    @Test
    void testFeedCommand() {
        FeedCommand cmd = new FeedCommand(plugin);
        Player player = Mockito.mock(Player.class);
        when(player.hasPermission("glowcmd.feed")).thenReturn(true);

        boolean res = cmd.onCommand(player, dummyCommand, "feed", new String[0]);
        assertTrue(res);
        verify(player).setFoodLevel(20);
        verify(player).setSaturation(20.0f);
        verify(messageManager).send(player, "feed.self");
    }

    @Test
    void testFlyCommand() {
        FlyCommand cmd = new FlyCommand(plugin);
        Player player = Mockito.mock(Player.class);
        when(player.hasPermission("glowcmd.fly")).thenReturn(true);
        when(player.getAllowFlight()).thenReturn(false);

        boolean res = cmd.onCommand(player, dummyCommand, "fly", new String[0]);
        assertTrue(res);
        verify(player).setAllowFlight(true);
        verify(messageManager).send(player, "fly.enabled");
    }

    @Test
    void testGamemodeCommandShorthand() {
        GamemodeCommand cmd = new GamemodeCommand(plugin);
        Player player = Mockito.mock(Player.class);
        when(player.hasPermission("glowcmd.gamemode")).thenReturn(true);

        boolean res = cmd.onCommand(player, dummyCommand, "gmc", new String[0]);
        assertTrue(res);
        verify(player).setGameMode(GameMode.CREATIVE);
        verify(messageManager).send(eq(player), eq("gamemode.changed"), any());
    }
}
