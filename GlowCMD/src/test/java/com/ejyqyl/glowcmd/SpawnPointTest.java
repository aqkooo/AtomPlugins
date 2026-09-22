package com.ejyqyl.glowcmd;

import com.ejyqyl.glowcmd.model.SpawnPoint;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SpawnPointTest {

    @Test
    void testSpawnPointModel() {
        SpawnPoint point = new SpawnPoint("world_nether", 12.5, 65.0, -99.5, 180.0f, 45.0f);

        assertEquals("world_nether", point.getWorldName());
        assertEquals(12.5, point.getX());
        assertEquals(65.0, point.getY());
        assertEquals(-99.5, point.getZ());
        assertEquals(180.0f, point.getYaw());
        assertEquals(45.0f, point.getPitch());
    }

    @Test
    void testFromLocation() {
        World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("custom_world");
        Location loc = new Location(world, 10.0, 70.0, 20.0, 90.0f, 0.0f);

        SpawnPoint point = SpawnPoint.fromLocation(loc);

        assertEquals("custom_world", point.getWorldName());
        assertEquals(10.0, point.getX());
        assertEquals(70.0, point.getY());
        assertEquals(20.0, point.getZ());
        assertEquals(90.0f, point.getYaw());
        assertEquals(0.0f, point.getPitch());
    }

    @Test
    void testEquality() {
        SpawnPoint p1 = new SpawnPoint("world", 10.0, 64.0, 10.0, 0.0f, 0.0f);
        SpawnPoint p2 = new SpawnPoint("world", 10.0, 64.0, 10.0, 0.0f, 0.0f);
        SpawnPoint p3 = new SpawnPoint("world_nether", 10.0, 64.0, 10.0, 0.0f, 0.0f);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1, p3);
    }
}
