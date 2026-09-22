package com.ejyqyl.glowcmd;

import com.ejyqyl.glowcmd.model.SpawnPoint;
import com.ejyqyl.glowcmd.spawn.SpawnManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SpawnManagerTest {

    @TempDir
    Path tempDir;

    private GlowCMD plugin;
    private File spawnFile;

    @BeforeEach
    void setup() {
        plugin = Mockito.mock(GlowCMD.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("GlowCMDSpawnTest"));
        when(plugin.isEnabled()).thenReturn(false); // test synchronous saves

        File dataDir = new File(tempDir.toFile(), "data");
        dataDir.mkdirs();
        spawnFile = new File(dataDir, "spawn.yml");
    }

    @Test
    void testSaveAndLoadSpawnPoints() {
        SpawnManager manager = new SpawnManager(plugin);
        assertNull(manager.getSpawn());
        assertNull(manager.getFirstSpawn());

        SpawnPoint mainSpawn = new SpawnPoint("world", 10.5, 65.0, -20.5, 90.0f, 0.0f);
        SpawnPoint firstSpawn = new SpawnPoint("lobby", 100.5, 70.0, 100.5, 180.0f, 10.0f);

        manager.setSpawn(mainSpawn);
        manager.setFirstSpawn(firstSpawn);
        manager.saveSync();

        assertTrue(spawnFile.exists());

        // Verify content in YAML file
        YamlConfiguration config = YamlConfiguration.loadConfiguration(spawnFile);
        assertEquals("world", config.getString("spawn.world"));
        assertEquals(10.5, config.getDouble("spawn.x"));
        assertEquals(65.0, config.getDouble("spawn.y"));
        assertEquals(-20.5, config.getDouble("spawn.z"));
        assertEquals(90.0f, (float) config.getDouble("spawn.yaw"));
        assertEquals(0.0f, (float) config.getDouble("spawn.pitch"));

        assertEquals("lobby", config.getString("first-spawn.world"));
        assertEquals(100.5, config.getDouble("first-spawn.x"));

        // Create a new manager instance and reload from file
        SpawnManager newManager = new SpawnManager(plugin);
        assertEquals(mainSpawn, newManager.getSpawn());
        assertEquals(firstSpawn, newManager.getFirstSpawn());
    }

    @Test
    void testMissingWorldDoesNotThrow() throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.set("spawn.world", "non_existent_world_404");
        config.set("spawn.x", 0.0);
        config.set("spawn.y", 64.0);
        config.set("spawn.z", 0.0);
        config.save(spawnFile);

        assertDoesNotThrow(() -> {
            SpawnManager manager = new SpawnManager(plugin);
            assertNotNull(manager.getSpawn());
            assertEquals("non_existent_world_404", manager.getSpawn().getWorldName());
            // toLocation will return null safely when world is not found on server
            assertNull(manager.getSpawn().toLocation());
        });
    }
}
