package me.ejyqyl.tituls.storage;

import me.ejyqyl.tituls.manager.TitulManager;
import me.ejyqyl.tituls.model.Titul;
import me.ejyqyl.tituls.model.TitulRarity;
import me.ejyqyl.tituls.model.TitulType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TitleStorage {
    private final DatabaseManager dbManager;

    public TitleStorage(@NotNull DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public CompletableFuture<List<Titul>> loadPlayerTituls(@NotNull UUID uuid, @NotNull TitulManager manager) {
        return CompletableFuture.supplyAsync(() -> {
            List<Titul> tituls = new ArrayList<>();
            String sql = "SELECT pt.title_id, pt.title_type, pt.unlocked_at, ct.display " +
                    "FROM player_titles pt " +
                    "LEFT JOIN custom_titles ct ON pt.title_id = ct.title_id AND pt.title_type = 'CUSTOM' " +
                    "WHERE pt.uuid = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String titleId = rs.getString("title_id");
                        TitulType type = TitulType.fromString(rs.getString("title_type"));
                        Long unlockedAt = rs.getLong("unlocked_at");
                        if (rs.wasNull()) {
                            unlockedAt = null;
                        }

                        if (type == TitulType.CASE) {
                            Titul template = manager.getTitul(titleId);
                            if (template != null) {
                                tituls.add(new Titul(titleId, template.getName(), TitulType.CASE, template.getRarity(), unlockedAt));
                            }
                        } else {
                            String display = rs.getString("display");
                            if (display == null) {
                                display = titleId;
                            }
                            tituls.add(new Titul(titleId, display, TitulType.CUSTOM, TitulRarity.RARE, unlockedAt));
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load player tituls for " + uuid, e);
            }
            return tituls;
        }, dbManager.getExecutor());
    }

    public CompletableFuture<Titul> loadActiveTitul(@NotNull UUID uuid, @NotNull TitulManager manager) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT pat.title_id, pat.title_type, pt.unlocked_at, ct.display " +
                    "FROM player_active_title pat " +
                    "JOIN player_titles pt ON pat.uuid = pt.uuid AND pat.title_id = pt.title_id AND pat.title_type = pt.title_type " +
                    "LEFT JOIN custom_titles ct ON pat.title_id = ct.title_id AND pat.title_type = 'CUSTOM' " +
                    "WHERE pat.uuid = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String titleId = rs.getString("title_id");
                        TitulType type = TitulType.fromString(rs.getString("title_type"));
                        Long unlockedAt = rs.getLong("unlocked_at");
                        if (rs.wasNull()) {
                            unlockedAt = null;
                        }

                        if (type == TitulType.CASE) {
                            Titul template = manager.getTitul(titleId);
                            if (template != null) {
                                return new Titul(titleId, template.getName(), TitulType.CASE, template.getRarity(), unlockedAt);
                            }
                        } else {
                            String display = rs.getString("display");
                            if (display == null) {
                                display = titleId;
                            }
                            return new Titul(titleId, display, TitulType.CUSTOM, TitulRarity.RARE, unlockedAt);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load active titul for " + uuid, e);
            }
            return null;
        }, dbManager.getExecutor());
    }

    public CompletableFuture<String> loadSortMode(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT sort_mode FROM player_settings WHERE uuid = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("sort_mode");
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load sort mode for " + uuid, e);
            }
            return null;
        }, dbManager.getExecutor());
    }

    public CompletableFuture<Void> setSortMode(@NotNull UUID uuid, @NotNull String sortMode) {
        return CompletableFuture.runAsync(() -> {
            String sql;
            if (dbManager.isMySQL()) {
                sql = "INSERT INTO player_settings (uuid, sort_mode) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE sort_mode = VALUES(sort_mode)";
            } else {
                sql = "INSERT INTO player_settings (uuid, sort_mode) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET sort_mode = excluded.sort_mode";
            }

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, sortMode);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to set sort mode for " + uuid, e);
            }
        }, dbManager.getExecutor());
    }

    public CompletableFuture<Void> addTitle(@NotNull UUID uuid, @NotNull String titleId, @NotNull TitulType type, @Nullable Long unlockedAt) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO player_titles (uuid, title_id, title_type, unlocked_at) VALUES (?, ?, ?, ?)";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, titleId);
                stmt.setString(3, type.name());
                if (unlockedAt != null) {
                    stmt.setLong(4, unlockedAt);
                } else {
                    stmt.setNull(4, Types.BIGINT);
                }
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to add title " + titleId + " for " + uuid, e);
            }
        }, dbManager.getExecutor());
    }

    public CompletableFuture<Void> updateUnlockedAt(@NotNull UUID uuid, @NotNull String titleId, long time) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE player_titles SET unlocked_at = ? WHERE uuid = ? AND title_id = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, time);
                stmt.setString(2, uuid.toString());
                stmt.setString(3, titleId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update unlocked_at for " + titleId + " of " + uuid, e);
            }
        }, dbManager.getExecutor());
    }

    public CompletableFuture<Void> removeTitle(@NotNull UUID uuid, @NotNull String titleId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_titles WHERE uuid = ? AND title_id = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, titleId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete title " + titleId + " for " + uuid, e);
            }
        }, dbManager.getExecutor());
    }

    public CompletableFuture<Void> setActiveTitle(@NotNull UUID uuid, @NotNull String titleId, @NotNull TitulType type) {
        return CompletableFuture.runAsync(() -> {
            String sql;
            if (dbManager.isMySQL()) {
                sql = "INSERT INTO player_active_title (uuid, title_id, title_type) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE title_id = VALUES(title_id), title_type = VALUES(title_type)";
            } else {
                sql = "INSERT INTO player_active_title (uuid, title_id, title_type) VALUES (?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET title_id = excluded.title_id, title_type = excluded.title_type";
            }

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, titleId);
                stmt.setString(3, type.name());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to set active title for " + uuid, e);
            }
        }, dbManager.getExecutor());
    }

    public CompletableFuture<Void> removeActiveTitle(@NotNull UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_active_title WHERE uuid = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to remove active title for " + uuid, e);
            }
        }, dbManager.getExecutor());
    }

    public CompletableFuture<Void> removeActiveTitleIfMatches(@NotNull UUID uuid, @NotNull String titleId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_active_title WHERE uuid = ? AND title_id = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, titleId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to remove active title if matches for " + uuid, e);
            }
        }, dbManager.getExecutor());
    }

    public CompletableFuture<Boolean> existsCustomTitle(@NotNull String titleId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM custom_titles WHERE title_id = ? LIMIT 1";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, titleId);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to check if custom title exists " + titleId, e);
            }
        }, dbManager.getExecutor());
    }

    public CompletableFuture<Void> createCustomTitle(@NotNull String titleId, @NotNull UUID uuid, @NotNull String display, long createdAt) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO custom_titles (title_id, uuid, display, created_at) VALUES (?, ?, ?, ?)";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, titleId);
                stmt.setString(2, uuid.toString());
                stmt.setString(3, display);
                stmt.setLong(4, createdAt);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to insert custom title " + titleId, e);
            }
        }, dbManager.getExecutor());
    }
}
