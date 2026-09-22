package com.ejyqyl.glowcmd;

import com.ejyqyl.glowcmd.manager.HomeManager;
import com.ejyqyl.glowcmd.manager.PrivateMessageManager;
import com.ejyqyl.glowcmd.manager.TeleportManager;
import com.ejyqyl.glowcmd.manager.WarpManager;
import com.ejyqyl.glowcmd.model.SpawnPoint;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ExtendedManagersTest {

    @TempDir
    Path tempDir;

    private GlowCMD plugin;

    @BeforeEach
    void setUp() {
        plugin = Mockito.mock(GlowCMD.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ExtendedManagersTest"));
        when(plugin.isEnabled()).thenReturn(false); // test synchronous saves
    }

    @Test
    void testHomeManager() {
        HomeManager manager = new HomeManager(plugin);
        UUID playerId = UUID.randomUUID();

        SpawnPoint home1 = new SpawnPoint("world", 10.0, 64.0, 10.0, 0f, 0f);
        SpawnPoint home2 = new SpawnPoint("world_nether", 50.0, 70.0, -50.0, 90f, 0f);

        manager.setHome(playerId, "home", home1);
        manager.setHome(playerId, "base", home2);
        manager.saveSync();

        assertEquals(home1, manager.getHome(playerId, "home"));
        assertEquals(home2, manager.getHome(playerId, "base"));
        assertEquals(2, manager.getHomes(playerId).size());

        // Test reloading from file
        HomeManager reloaded = new HomeManager(plugin);
        assertEquals(home1, reloaded.getHome(playerId, "home"));
        assertEquals(home2, reloaded.getHome(playerId, "base"));

        // Delete home
        assertTrue(reloaded.deleteHome(playerId, "base"));
        assertNull(reloaded.getHome(playerId, "base"));
        assertEquals(1, reloaded.getHomes(playerId).size());
    }

    @Test
    void testHomeLimits() {
        HomeManager manager = new HomeManager(plugin);
        Player player = Mockito.mock(Player.class);

        // Default: 3
        when(player.getEffectivePermissions()).thenReturn(Set.of());
        assertEquals(3, manager.getMaxHomes(player));

        // Permission: glowcmd.homes.5
        PermissionAttachmentInfo pai = new PermissionAttachmentInfo(player, "glowcmd.homes.5", null, true);
        when(player.getEffectivePermissions()).thenReturn(Set.of(pai));
        assertEquals(5, manager.getMaxHomes(player));

        // Unlimited
        when(player.hasPermission("glowcmd.homes.unlimited")).thenReturn(true);
        assertEquals(1000, manager.getMaxHomes(player));
    }

    @Test
    void testWarpManager() {
        WarpManager manager = new WarpManager(plugin);

        SpawnPoint shop = new SpawnPoint("world", 100.0, 65.0, 200.0, 180f, 0f);
        manager.setWarp("shop", shop);
        manager.saveSync();

        assertEquals(shop, manager.getWarp("shop"));
        assertEquals(1, manager.getWarps().size());

        // Test reloading
        WarpManager reloaded = new WarpManager(plugin);
        assertEquals(shop, reloaded.getWarp("shop"));

        // Delete
        assertTrue(reloaded.deleteWarp("shop"));
        assertNull(reloaded.getWarp("shop"));
    }

    @Test
    void testTeleportManager() {
        TeleportManager manager = new TeleportManager(plugin);
        Player sender = Mockito.mock(Player.class);
        Player target = Mockito.mock(Player.class);

        UUID sId = UUID.randomUUID();
        UUID tId = UUID.randomUUID();

        when(sender.getUniqueId()).thenReturn(sId);
        when(sender.getName()).thenReturn("Sender");
        when(target.getUniqueId()).thenReturn(tId);
        when(target.getName()).thenReturn("Target");

        // TPA send
        manager.sendRequest(sender, target, false);

        TeleportManager.TpaRequest req = manager.getLatestIncomingRequest(tId);
        assertNotNull(req);
        assertEquals(sId, req.senderId());
        assertEquals(tId, req.targetId());
        assertFalse(req.isHere());
        assertFalse(req.isExpired());

        // Cancel
        assertNotNull(manager.cancelOutgoingRequest(sId));
        assertNull(manager.getLatestIncomingRequest(tId));
    }

    @Test
    void testPrivateMessageManager() {
        PrivateMessageManager manager = new PrivateMessageManager();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        manager.setReplyTarget(p1, p2);
        assertEquals(p2, manager.getReplyTarget(p1));
        assertEquals(p1, manager.getReplyTarget(p2));

        manager.removePlayer(p1);
        assertNull(manager.getReplyTarget(p1));
        assertNull(manager.getReplyTarget(p2));
    }
}
