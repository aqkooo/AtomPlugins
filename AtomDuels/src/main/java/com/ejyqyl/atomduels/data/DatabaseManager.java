package com.ejyqyl.atomduels.data;

import com.ejyqyl.atomduels.util.ItemSerializer;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * SQLite database manager handling player stats, transaction history, and active duel persistence.
 * Authored by ejyqyl.
 */
public class DatabaseManager {

    private final Plugin plugin;
    private final File dbFile;
    private Connection connection;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "AtomDuels-DatabasePool");
        thread.setDaemon(true);
        return thread;
    });

    public record ActiveDuelRecord(UUID duelId, UUID player1, UUID player2, double betAmount, String mode, String kit, String arena, long createdAt) {}

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "database.db");
    }

    public synchronized void init() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
            plugin.getLogger().info("SQLite database connected successfully.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to SQLite database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(32) NOT NULL,
                    elo INT NOT NULL DEFAULT 1000,
                    wins INT NOT NULL DEFAULT 0,
                    losses INT NOT NULL DEFAULT 0,
                    draws INT NOT NULL DEFAULT 0,
                    win_streak INT NOT NULL DEFAULT 0,
                    best_streak INT NOT NULL DEFAULT 0,
                    calibration_matches INT NOT NULL DEFAULT 0,
                    money_won DOUBLE NOT NULL DEFAULT 0,
                    money_lost DOUBLE NOT NULL DEFAULT 0,
                    claim_data TEXT,
                    loot_data TEXT
                );
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS active_duels (
                    duel_id VARCHAR(36) PRIMARY KEY,
                    player1_uuid VARCHAR(36) NOT NULL,
                    player2_uuid VARCHAR(36) NOT NULL,
                    bet_amount DOUBLE NOT NULL DEFAULT 0,
                    mode VARCHAR(32) NOT NULL,
                    kit_name VARCHAR(64),
                    arena_name VARCHAR(64),
                    created_at BIGINT NOT NULL
                );
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    duel_id VARCHAR(36),
                    player_uuid VARCHAR(36) NOT NULL,
                    amount DOUBLE NOT NULL,
                    type VARCHAR(32) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    attempts INT NOT NULL DEFAULT 1,
                    created_at BIGINT NOT NULL
                );
            """);
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        }
        return connection;
    }

    public void runAsync(Runnable task) {
        dbExecutor.submit(task);
    }

    public synchronized PlayerData loadPlayerData(UUID uuid, String usernameFallback) {
        String sql = "SELECT * FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    PlayerData data = new PlayerData(uuid, rs.getString("username"));
                    data.setElo(rs.getInt("elo"));
                    data.setWins(rs.getInt("wins"));
                    data.setLosses(rs.getInt("losses"));
                    data.setDraws(rs.getInt("draws"));
                    data.setWinStreak(rs.getInt("win_streak"));
                    data.setBestStreak(rs.getInt("best_streak"));
                    data.setCalibrationMatches(rs.getInt("calibration_matches"));
                    data.setMoneyWon(rs.getDouble("money_won"));
                    data.setMoneyLost(rs.getDouble("money_lost"));
                    data.getClaimItems().addAll(ItemSerializer.fromBase64(rs.getString("claim_data")));
                    data.getLoserLootItems().addAll(ItemSerializer.fromBase64(rs.getString("loot_data")));
                    return data;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load player data for " + uuid, e);
        }
        return new PlayerData(uuid, usernameFallback != null ? usernameFallback : "Unknown");
    }

    public synchronized void savePlayerData(PlayerData data) {
        String sql = """
            INSERT INTO players (uuid, username, elo, wins, losses, draws, win_streak, best_streak, calibration_matches, money_won, money_lost, claim_data, loot_data)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                username = excluded.username,
                elo = excluded.elo,
                wins = excluded.wins,
                losses = excluded.losses,
                draws = excluded.draws,
                win_streak = excluded.win_streak,
                best_streak = excluded.best_streak,
                calibration_matches = excluded.calibration_matches,
                money_won = excluded.money_won,
                money_lost = excluded.money_lost,
                claim_data = excluded.claim_data,
                loot_data = excluded.loot_data;
        """;
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, data.getUuid().toString());
            stmt.setString(2, data.getUsername());
            stmt.setInt(3, data.getElo());
            stmt.setInt(4, data.getWins());
            stmt.setInt(5, data.getLosses());
            stmt.setInt(6, data.getDraws());
            stmt.setInt(7, data.getWinStreak());
            stmt.setInt(8, data.getBestStreak());
            stmt.setInt(9, data.getCalibrationMatches());
            stmt.setDouble(10, data.getMoneyWon());
            stmt.setDouble(11, data.getMoneyLost());
            stmt.setString(12, ItemSerializer.toBase64(data.getClaimItems()));
            stmt.setString(13, ItemSerializer.toBase64(data.getLoserLootItems()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data for " + data.getUuid(), e);
        }
    }

    public synchronized List<PlayerData> getTopByElo(int limit) {
        List<PlayerData> top = new ArrayList<>();
        String sql = "SELECT * FROM players ORDER BY elo DESC LIMIT ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PlayerData data = new PlayerData(UUID.fromString(rs.getString("uuid")), rs.getString("username"));
                    data.setElo(rs.getInt("elo"));
                    data.setWins(rs.getInt("wins"));
                    data.setLosses(rs.getInt("losses"));
                    top.add(data);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not query top elo", e);
        }
        return top;
    }

    public synchronized List<PlayerData> getTopByWins(int limit) {
        List<PlayerData> top = new ArrayList<>();
        String sql = "SELECT * FROM players ORDER BY wins DESC LIMIT ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PlayerData data = new PlayerData(UUID.fromString(rs.getString("uuid")), rs.getString("username"));
                    data.setElo(rs.getInt("elo"));
                    data.setWins(rs.getInt("wins"));
                    data.setLosses(rs.getInt("losses"));
                    top.add(data);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not query top wins", e);
        }
        return top;
    }

    public synchronized void recordActiveDuel(UUID duelId, UUID p1, UUID p2, double bet, String mode, String kit, String arena) {
        String sql = "INSERT OR REPLACE INTO active_duels (duel_id, player1_uuid, player2_uuid, bet_amount, mode, kit_name, arena_name, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, duelId.toString());
            stmt.setString(2, p1 != null ? p1.toString() : "");
            stmt.setString(3, p2 != null ? p2.toString() : "");
            stmt.setDouble(4, bet);
            stmt.setString(5, mode);
            stmt.setString(6, kit != null ? kit : "");
            stmt.setString(7, arena != null ? arena : "");
            stmt.setLong(8, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not record active duel " + duelId, e);
        }
    }

    public synchronized void removeActiveDuel(UUID duelId) {
        String sql = "DELETE FROM active_duels WHERE duel_id = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, duelId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not delete active duel " + duelId, e);
        }
    }

    public synchronized List<ActiveDuelRecord> getPendingActiveDuels() {
        List<ActiveDuelRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM active_duels";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID duelId = UUID.fromString(rs.getString("duel_id"));
                UUID p1 = parseUuidSafe(rs.getString("player1_uuid"));
                UUID p2 = parseUuidSafe(rs.getString("player2_uuid"));
                double bet = rs.getDouble("bet_amount");
                String mode = rs.getString("mode");
                String kit = rs.getString("kit_name");
                String arena = rs.getString("arena_name");
                long createdAt = rs.getLong("created_at");
                list.add(new ActiveDuelRecord(duelId, p1, p2, bet, mode, kit, arena, createdAt));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not fetch pending active duels", e);
        }
        return list;
    }

    public synchronized void recordTransaction(UUID duelId, UUID player, double amount, String type, String status, int attempts) {
        String sql = "INSERT INTO transactions (duel_id, player_uuid, amount, type, status, attempts, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, duelId != null ? duelId.toString() : null);
            stmt.setString(2, player.toString());
            stmt.setDouble(3, amount);
            stmt.setString(4, type);
            stmt.setString(5, status);
            stmt.setInt(6, attempts);
            stmt.setLong(7, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not record transaction", e);
        }
    }

    public void close() {
        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                dbExecutor.shutdownNow();
            }
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error closing database", e);
        }
    }

    private UUID parseUuidSafe(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
