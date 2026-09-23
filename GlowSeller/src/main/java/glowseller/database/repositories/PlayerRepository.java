package glowseller.database.repositories;

import glowseller.database.DataBaseManager;
import glowseller.configs.impl.DataBaseConfig;
import glowseller.models.PlayerData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerRepository {
    private final DataBaseManager dbManager;
    private final Logger logger;
    private final Object writeLock = new Object();

    public PlayerRepository(DataBaseManager dbManager, Logger logger) {
        this.dbManager = dbManager;
        this.logger = logger;
    }

    public PlayerData load(UUID uuid) {
        if (uuid == null) return null;

        PlayerData data = null;
        String playerSql = "SELECT points, active_booster_key, booster_expire_at, auto_sell_mode, auto_sell_filter FROM player_data WHERE uuid = ?";
        String historySql = "SELECT item_key, COUNT(*), MAX(purchased_at) FROM purchase_log WHERE player_uuid = ? GROUP BY item_key";

        try (Connection conn = dbManager.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(playerSql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long points = rs.getLong("points");
                        String boosterKey = rs.getString("active_booster_key");
                        long expireAt = rs.getLong("booster_expire_at");
                        String modeStr = rs.getString("auto_sell_mode");
                        PlayerData.AutoSellMode mode = PlayerData.AutoSellMode.OFF;
                        if (modeStr != null) {
                            try {
                                mode = PlayerData.AutoSellMode.valueOf(modeStr);
                            } catch (Exception ignored) {}
                        }
                        if (mode == PlayerData.AutoSellMode.ENABLED_SESSION) {
                            mode = PlayerData.AutoSellMode.OFF;
                        }
                        String filterStr = rs.getString("auto_sell_filter");
                        List<String> filter = (filterStr != null && !filterStr.isEmpty())
                                ? Arrays.asList(filterStr.split(","))
                                : Collections.emptyList();

                        data = new PlayerData(uuid, points, boosterKey, expireAt, mode, filter);
                    }
                }
            }

            if (data == null) {
                data = new PlayerData(uuid);
            }

            try (PreparedStatement ps2 = conn.prepareStatement(historySql)) {
                ps2.setString(1, uuid.toString());
                try (ResultSet rs2 = ps2.executeQuery()) {
                    Map<String, Integer> counts = new HashMap<>();
                    Map<String, Long> lastTimes = new HashMap<>();
                    while (rs2.next()) {
                        String itemKey = rs2.getString(1);
                        int count = rs2.getInt(2);
                        long lastTime = rs2.getLong(3);
                        counts.put(itemKey, count);
                        lastTimes.put(itemKey, lastTime);
                    }
                    data.initPurchaseHistory(counts, lastTimes);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load player data for " + uuid, e);
        }

        return data != null ? data : new PlayerData(uuid);
    }

    public void save(PlayerData data) {
        if (data == null || data.getUuid() == null) return;

        synchronized (writeLock) {
            String sql = getUpsertSql();

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, data.getUuid().toString());
                ps.setLong(2, data.getPoints());
                ps.setString(3, data.getActiveBoosterKey());
                ps.setLong(4, data.getBoosterExpireAt());
                ps.setString(5, data.getAutoSellMode().name());
                ps.setString(6, String.join(",", data.getAutoSellFilter()));
                ps.setLong(7, System.currentTimeMillis());

                ps.executeUpdate();
                data.setDirty(false);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save player data for " + data.getUuid(), e);
            }
        }
    }

    public void saveAll(Collection<PlayerData> collection) {
        if (collection == null || collection.isEmpty()) return;

        List<PlayerData> dirtyList = new ArrayList<>();
        for (PlayerData d : collection) {
            if (d.isDirty()) {
                dirtyList.add(d);
            }
        }
        if (dirtyList.isEmpty()) return;

        synchronized (writeLock) {
            String sql = getUpsertSql();

            try (Connection conn = dbManager.getConnection()) {
                boolean oldAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    long now = System.currentTimeMillis();
                    for (PlayerData data : dirtyList) {
                        ps.setString(1, data.getUuid().toString());
                        ps.setLong(2, data.getPoints());
                        ps.setString(3, data.getActiveBoosterKey());
                        ps.setLong(4, data.getBoosterExpireAt());
                        ps.setString(5, data.getAutoSellMode().name());
                        ps.setString(6, String.join(",", data.getAutoSellFilter()));
                        ps.setLong(7, now);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    conn.commit();
                    for (PlayerData d : dirtyList) {
                        d.setDirty(false);
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(oldAutoCommit);
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to batch save player data", e);
            }
        }
    }

    private String getUpsertSql() {
        DataBaseConfig.Type type = dbManager.getDbType();
        if (type == DataBaseConfig.Type.MYSQL) {
            return """
                INSERT INTO player_data (uuid, points, active_booster_key, booster_expire_at, auto_sell_mode, auto_sell_filter, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    points = VALUES(points),
                    active_booster_key = VALUES(active_booster_key),
                    booster_expire_at = VALUES(booster_expire_at),
                    auto_sell_mode = VALUES(auto_sell_mode),
                    auto_sell_filter = VALUES(auto_sell_filter),
                    updated_at = VALUES(updated_at)
                """;
        } else {
            return """
                INSERT INTO player_data (uuid, points, active_booster_key, booster_expire_at, auto_sell_mode, auto_sell_filter, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    points = excluded.points,
                    active_booster_key = excluded.active_booster_key,
                    booster_expire_at = excluded.booster_expire_at,
                    auto_sell_mode = excluded.auto_sell_mode,
                    auto_sell_filter = excluded.auto_sell_filter,
                    updated_at = excluded.updated_at
                """;
        }
    }
}
