package ru.atomicsqd.atomreactor;

import org.junit.jupiter.api.Test;
import ru.atomicsqd.atomreactor.config.ReactorLevel;
import ru.atomicsqd.atomreactor.model.GenerationTarget;
import ru.atomicsqd.atomreactor.model.Reactor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ReactorModelTest {

    @Test
    public void testReactorLevel() {
        ReactorLevel level = new ReactorLevel(1, "Test Lvl 1", 50.0, 20, 15, 1000.0, 5000.0, "DUST", "#FF0000");

        assertEquals(1, level.getLevel());
        assertEquals("Test Lvl 1", level.getName());
        assertEquals(50.0, level.getIncome());
        assertEquals(20, level.getIntervalSeconds());
        assertEquals(15, level.getRadius());
        assertEquals(1000.0, level.getUpgradeCost());
        assertEquals(5000.0, level.getMaxStorage());
    }

    @Test
    public void testReactorStorage() {
        UUID owner = UUID.randomUUID();
        Reactor reactor = new Reactor(UUID.randomUUID(), owner, "Player1", "world", 100, 64, 200, 1);

        assertEquals(0.0, reactor.getStoredBalance());
        reactor.depositStored(500.0, 1000.0);
        assertEquals(500.0, reactor.getStoredBalance());

        // Test max storage clamp
        reactor.depositStored(800.0, 1000.0);
        assertEquals(1000.0, reactor.getStoredBalance());

        double withdrawn = reactor.withdrawAllStored();
        assertEquals(1000.0, withdrawn);
        assertEquals(0.0, reactor.getStoredBalance());
    }

    @Test
    public void testGenerationTarget() {
        assertEquals(GenerationTarget.OWNER_GLOBAL, GenerationTarget.fromString("OWNER_GLOBAL"));
        assertEquals(GenerationTarget.OWNER_IN_RADIUS, GenerationTarget.fromString("OWNER_IN_RADIUS"));
        assertEquals(GenerationTarget.ALL_IN_RADIUS, GenerationTarget.fromString("ALL_IN_RADIUS"));
        assertEquals(GenerationTarget.INTERNAL_VAULT, GenerationTarget.fromString("INTERNAL_VAULT"));
        assertEquals(GenerationTarget.OWNER_GLOBAL, GenerationTarget.fromString("UNKNOWN"));
    }
}
