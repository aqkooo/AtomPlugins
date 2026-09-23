package glowseller;

import glowseller.configs.impl.DataBaseConfig;
import glowseller.database.DataBaseManager;
import glowseller.database.repositories.PlayerRepository;
import glowseller.database.repositories.PurchaseLogRepository;
import glowseller.models.PlayerData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseSchemaAndRepositoryTest {
    private File tempDb;
    private DataBaseManager mockDbManager;
    private PlayerRepository playerRepository;
    private PurchaseLogRepository purchaseLogRepository;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        tempDb = File.createTempFile("glowseller_test_", ".db");
        tempDb.deleteOnExit();

        String url = "jdbc:sqlite:" + tempDb.getAbsolutePath();

        // Execute DDL
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid VARCHAR(36) PRIMARY KEY,
                    points BIGINT NOT NULL DEFAULT 0,
                    active_booster_key VARCHAR(64),
                    booster_expire_at BIGINT NOT NULL DEFAULT 0,
                    auto_sell_mode VARCHAR(32) DEFAULT 'OFF',
                    auto_sell_filter TEXT DEFAULT '',
                    updated_at BIGINT
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS purchase_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(32),
                    item_key VARCHAR(64) NOT NULL,
                    category VARCHAR(64),
                    price BIGINT NOT NULL,
                    purchased_at BIGINT NOT NULL
                );
            """);
        }

        mockDbManager = Mockito.mock(DataBaseManager.class);
        Mockito.when(mockDbManager.getConnection()).thenAnswer(inv -> DriverManager.getConnection(url));
        Mockito.when(mockDbManager.getDbType()).thenReturn(DataBaseConfig.Type.SQLITE);

        Logger logger = Logger.getLogger("GlowSellerTest");
        playerRepository = new PlayerRepository(mockDbManager, logger);
        purchaseLogRepository = new PurchaseLogRepository(mockDbManager, logger);
    }

    @AfterEach
    void tearDown() {
        if (tempDb != null && tempDb.exists()) {
            tempDb.delete();
        }
    }

    @Test
    @DisplayName("Verify PlayerRepository insert, update and load")
    void testPlayerRepository() {
        UUID uuid = UUID.randomUUID();

        // 1. Initial load for non-existing player should return default data with 0 points
        PlayerData initial = playerRepository.load(uuid);
        assertNotNull(initial);
        assertEquals(uuid, initial.getUuid());
        assertEquals(0L, initial.getPoints());
        assertNull(initial.getActiveBoosterKey());

        // 2. Modify and save
        initial.addPoints(1500L);
        initial.setActiveBoosterKey("coin_x2_1h");
        long expireAt = System.currentTimeMillis() + 3600_000L;
        initial.setBoosterExpireAt(expireAt);

        playerRepository.save(initial);
        assertFalse(initial.isDirty());

        // 3. Reload from DB
        PlayerData loaded = playerRepository.load(uuid);
        assertNotNull(loaded);
        assertEquals(1500L, loaded.getPoints());
        assertEquals("coin_x2_1h", loaded.getActiveBoosterKey());
        assertEquals(expireAt, loaded.getBoosterExpireAt());

        // 4. Update points and clear booster
        loaded.takePoints(500L);
        loaded.clearBooster();
        playerRepository.save(loaded);

        PlayerData updated = playerRepository.load(uuid);
        assertEquals(1000L, updated.getPoints());
        assertNull(updated.getActiveBoosterKey());
    }

    @Test
    @DisplayName("Verify PurchaseLogRepository log and count limits")
    void testPurchaseLogRepository() {
        UUID uuid = UUID.randomUUID();
        String playerName = "Player1";

        // Count before purchases
        int initialCount = purchaseLogRepository.countPurchases(uuid, "case_key", 0L);
        assertEquals(0, initialCount);

        long now = System.currentTimeMillis();

        // Log first purchase
        purchaseLogRepository.logPurchase(uuid, playerName, "case_key", "items", 1000L, now);

        int countAfter1 = purchaseLogRepository.countPurchases(uuid, "case_key", 0L);
        assertEquals(1, countAfter1);

        // Log second purchase
        purchaseLogRepository.logPurchase(uuid, playerName, "case_key", "items", 1000L, now + 1000);
        int countAfter2 = purchaseLogRepository.countPurchases(uuid, "case_key", 0L);
        assertEquals(2, countAfter2);

        // Count for another item
        assertEquals(0, purchaseLogRepository.countPurchases(uuid, "xp_bottle", 0L));

        // Count with timestamp filter (since 5 seconds after second purchase)
        assertEquals(0, purchaseLogRepository.countPurchases(uuid, "case_key", now + 10_000));
    }
}
