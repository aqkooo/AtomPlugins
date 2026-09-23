package glowseller.database.repositories;

import glowseller.database.DataBaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PurchaseLogRepository {
    private final DataBaseManager dbManager;
    private final Logger logger;

    public PurchaseLogRepository(DataBaseManager dbManager, Logger logger) {
        this.dbManager = dbManager;
        this.logger = logger;
    }

    public void logPurchase(UUID playerUuid, String playerName, String itemKey, String category, long price, long timestamp) {
        if (playerUuid == null || itemKey == null) return;

        String sql = """
            INSERT INTO purchase_log (player_uuid, player_name, item_key, category, price, purchased_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.setString(3, itemKey);
            ps.setString(4, category);
            ps.setLong(5, price);
            ps.setLong(6, timestamp);

            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to log purchase for " + playerUuid, e);
        }
    }

    public int countPurchases(UUID playerUuid, String itemKey, long sinceTimestamp) {
        if (playerUuid == null || itemKey == null) return 0;

        String sql = "SELECT COUNT(*) FROM purchase_log WHERE player_uuid = ? AND item_key = ? AND purchased_at >= ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, itemKey);
            ps.setLong(3, sinceTimestamp);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to count purchases for " + playerUuid, e);
        }

        return 0;
    }
}
