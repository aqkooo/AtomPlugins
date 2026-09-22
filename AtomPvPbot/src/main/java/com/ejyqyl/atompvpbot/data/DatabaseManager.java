package com.ejyqyl.atompvpbot.data;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SQLite persistence layer for AtomPvPbot player statistics.
 *
 * @author ejyqyl
 */
public class DatabaseManager {

    private final Plugin plugin;
    private Connection connection;
    private final File dbFile;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "database.db");
    }

    public void init() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to SQLite database", e);
        }
    }

    private void createTables() {
        String sql = """
            CREATE TABLE IF NOT EXISTS player_stats (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(16) NOT NULL,
                wins INTEGER DEFAULT 0,
                losses INTEGER DEFAULT 0,
                win_streak INTEGER DEFAULT 0,
                best_streak INTEGER DEFAULT 0,
                total_games INTEGER DEFAULT 0
            );
        """;

        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite tables", e);
        }
    }

    public synchronized PlayerData loadPlayerData(UUID uuid, String username) {
        PlayerData data = new PlayerData(uuid, username);
        if (connection == null) return data;

        String query = "SELECT * FROM player_stats WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                data.setUsername(rs.getString("username"));
                data.setWins(rs.getInt("wins"));
                data.setLosses(rs.getInt("losses"));
                data.setWinStreak(rs.getInt("win_streak"));
                data.setBestStreak(rs.getInt("best_streak"));
                data.setTotalGames(rs.getInt("total_games"));
            } else {
                savePlayerData(data);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load player stats for " + uuid, e);
        }
        return data;
    }

    public synchronized void savePlayerData(PlayerData data) {
        if (connection == null || data == null) return;

        String upsert = """
            INSERT INTO player_stats (uuid, username, wins, losses, win_streak, best_streak, total_games)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                username = excluded.username,
                wins = excluded.wins,
                losses = excluded.losses,
                win_streak = excluded.win_streak,
                best_streak = excluded.best_streak,
                total_games = excluded.total_games;
        """;

        try (PreparedStatement ps = connection.prepareStatement(upsert)) {
            ps.setString(1, data.getUuid().toString());
            ps.setString(2, data.getUsername());
            ps.setInt(3, data.getWins());
            ps.setInt(4, data.getLosses());
            ps.setInt(5, data.getWinStreak());
            ps.setInt(6, data.getBestStreak());
            ps.setInt(7, data.getTotalGames());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save player stats for " + data.getUuid(), e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }
}
