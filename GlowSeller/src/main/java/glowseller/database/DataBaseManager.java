package glowseller.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import glowseller.Main;
import glowseller.configs.impl.DataBaseConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class DataBaseManager {
    private final Main plugin;
    private HikariDataSource dataSource;
    private final DataBaseConfig.Type dbType;

    public DataBaseManager(Main plugin) {
        this.plugin = plugin;
        this.dbType = plugin.getDataBaseConfig().getType();
        initPool();
        initTables();
    }

    private void initPool() {
        DataBaseConfig cfg = plugin.getDataBaseConfig();
        HikariConfig hikari = new HikariConfig();

        switch (cfg.getType()) {
            case SQLITE -> {
                File dbFile = new File(plugin.getDataFolder(), cfg.getSqliteFileName());
                hikari.setDriverClassName("org.sqlite.JDBC");
                hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
                hikari.setMaximumPoolSize(1);
                hikari.setConnectionTestQuery("SELECT 1");
                hikari.addDataSourceProperty("busy_timeout", "10000");
            }
            case MYSQL -> {
                hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
                hikari.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true&characterEncoding=utf8",
                        cfg.getHost(), cfg.getPort(), cfg.getDatabase()));
                hikari.setUsername(cfg.getUsername());
                hikari.setPassword(cfg.getPassword());
                hikari.setMaximumPoolSize(cfg.getPoolSize());
                hikari.setConnectionTimeout(cfg.getConnectionTimeout());
            }
            case POSTGRESQL -> {
                hikari.setDriverClassName("org.postgresql.Driver");
                hikari.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s",
                        cfg.getHost(), cfg.getPort(), cfg.getDatabase()));
                hikari.setUsername(cfg.getUsername());
                hikari.setPassword(cfg.getPassword());
                hikari.setMaximumPoolSize(cfg.getPoolSize());
                hikari.setConnectionTimeout(cfg.getConnectionTimeout());
            }
        }

        hikari.setPoolName("GlowSeller-Pool");
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(hikari);
    }

    private void initTables() {
        String autoIncrement = switch (dbType) {
            case SQLITE -> "INTEGER PRIMARY KEY AUTOINCREMENT";
            case MYSQL -> "BIGINT AUTO_INCREMENT PRIMARY KEY";
            case POSTGRESQL -> "BIGSERIAL PRIMARY KEY";
        };

        String createPlayerData = """
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid VARCHAR(36) PRIMARY KEY,
                    points BIGINT NOT NULL DEFAULT 0,
                    active_booster_key VARCHAR(64),
                    booster_expire_at BIGINT NOT NULL DEFAULT 0,
                    auto_sell_mode VARCHAR(32) DEFAULT 'OFF',
                    auto_sell_filter TEXT DEFAULT '',
                    updated_at BIGINT
                );
                """;

        String createPurchaseLog = String.format("""
                CREATE TABLE IF NOT EXISTS purchase_log (
                    id %s,
                    player_uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(32),
                    item_key VARCHAR(64) NOT NULL,
                    category VARCHAR(64),
                    price BIGINT NOT NULL,
                    purchased_at BIGINT NOT NULL
                );
                """, autoIncrement);

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.execute(createPlayerData);
            st.execute(createPurchaseLog);
            // Safe migrations if table already existed without new columns
            try {
                st.execute("ALTER TABLE player_data ADD COLUMN auto_sell_mode VARCHAR(32) DEFAULT 'OFF'");
            } catch (SQLException ignored) {}
            try {
                st.execute("ALTER TABLE player_data ADD COLUMN auto_sell_filter TEXT DEFAULT ''");
            } catch (SQLException ignored) {}
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database tables", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource is closed or not initialized");
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public DataBaseConfig.Type getDbType() {
        return dbType;
    }
}
