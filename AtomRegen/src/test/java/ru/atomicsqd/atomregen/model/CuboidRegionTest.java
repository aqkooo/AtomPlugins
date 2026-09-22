package ru.atomicsqd.atomregen.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CuboidRegionTest {

    @Test
    public void testContains() {
        CuboidRegion region = new CuboidRegion("arena", "world", 0, 10, 0, 10, 20, 10, 15);

        assertTrue(region.contains(5, 15, 5, "world"));
        assertTrue(region.contains(0, 10, 0, "world"));
        assertTrue(region.contains(10, 20, 10, "world"));

        assertFalse(region.contains(11, 15, 5, "world"));
        assertFalse(region.contains(5, 9, 5, "world"));
        assertFalse(region.contains(5, 15, 5, "world_nether"));
    }

    @Test
    public void testVolume() {
        CuboidRegion region = new CuboidRegion("test", "world", 0, 0, 0, 2, 2, 2, 10);
        // (2 - 0 + 1) * (2 - 0 + 1) * (2 - 0 + 1) = 3 * 3 * 3 = 27
        assertEquals(27, region.getVolume());
    }

    @Test
    public void testBlockVectorSerialization() {
        BlockVector vec = new BlockVector(10, -5, 100);
        String serialized = vec.serialize();
        assertEquals("10,-5,100", serialized);

        BlockVector parsed = BlockVector.fromString(serialized);
        assertEquals(vec, parsed);
    }
}
