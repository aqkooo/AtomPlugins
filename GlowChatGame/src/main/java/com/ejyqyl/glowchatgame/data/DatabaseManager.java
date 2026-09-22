package com.ejyqyl.glowchatgame.data;

import com.ejyqyl.glowchatgame.GlowChatGame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * High-performance, asynchronous SQLite database manager.
 * Safely handles connection pooling, schema initialization, and async read/write operations.
 *
 * @author ejyqyl, Glowdevv
 */
public final class DatabaseManager {

    private final GlowChatGame plugin;
    private final File dbFile;
    private Connection connection;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public DatabaseManager(@NotNull GlowChatGame plugin, @NotNull String dbPath) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), dbPath);
        init();
    }

    private void init() {
        try {
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_stats (
                        uuid VARCHAR(36) PRIMARY KEY,
                        player_name VARCHAR(32) NOT NULL,
                        wins INTEGER NOT NULL DEFAULT 0,
                        games INTEGER NOT NULL DEFAULT 0,
                        rewards_claimed INTEGER NOT NULL DEFAULT 0,
                        last_win_timestamp BIGINT NOT NULL DEFAULT 0
                    );
                """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_player_name ON player_stats (player_name COLLATE NOCASE);");
            }
            plugin.getLogger().info("SQLite database connected successfully: " + dbFile.getName());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite database: " + e.getMessage(), e);
        }
    }

    @NotNull
    public CompletableFuture<PlayerStats> loadStats(@NotNull UUID uuid, @NotNull String defaultName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConnection();
                String sql = "SELECT * FROM player_stats WHERE uuid = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return new PlayerStats(
                                    uuid,
                                    rs.getString("player_name"),
                                    rs.getInt("wins"),
                                    rs.getInt("games"),
                                    rs.getInt("rewards_claimed"),
                                    rs.getLong("last_win_timestamp")
                            );
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to load player stats for " + uuid + ": " + e.getMessage());
            }
            return new PlayerStats(uuid, defaultName);
        }, dbExecutor);
    }

    @NotNull
    public CompletableFuture<PlayerStats> loadStatsByName(@NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConnection();
                String sql = "SELECT * FROM player_stats WHERE player_name = ? COLLATE NOCASE LIMIT 1";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, playerName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            return new PlayerStats(
                                    uuid,
                                    rs.getString("player_name"),
                                    rs.getInt("wins"),
                                    rs.getInt("games"),
                                    rs.getInt("rewards_claimed"),
                                    rs.getLong("last_win_timestamp")
                            );
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to load player stats by name " + playerName + ": " + e.getMessage());
            }
            return null;
        }, dbExecutor);
    }

    @NotNull
    public CompletableFuture<Void> saveStats(@NotNull PlayerStats stats) {
        return CompletableFuture.runAsync(() -> {
            try {
                ensureConnection();
                String sql = """
                    INSERT INTO player_stats (uuid, player_name, wins, games, rewards_claimed, last_win_timestamp)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        player_name = excluded.player_name,
                        wins = excluded.wins,
                        games = excluded.games,
                        rewards_claimed = excluded.rewards_claimed,
                        last_win_timestamp = excluded.last_win_timestamp;
                """;
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, stats.getUuid().toString());
                    ps.setString(2, stats.getPlayerName());
                    ps.setInt(3, stats.getWins());
                    ps.setInt(4, stats.getGames());
                    ps.setInt(5, stats.getRewardsClaimed());
                    ps.setLong(6, stats.getLastWinTimestamp());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save player stats for " + stats.getUuid() + ": " + e.getMessage());
            }
        }, dbExecutor);
    }

    private synchronized void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        }
    }

    public void close() {
        dbExecutor.shutdown();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
