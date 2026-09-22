package me.ejyqyl.tituls.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.ejyqyl.tituls.AtomTitulsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class DatabaseManager implements AutoCloseable {
    private final AtomTitulsPlugin plugin;
    private final boolean isMySQL;
    private HikariDataSource dataSource;
    private final ExecutorService executor;

    public DatabaseManager(AtomTitulsPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        String type = config.getString("type", "sqlite").trim().toLowerCase();
        this.isMySQL = type.equalsIgnoreCase("mysql");

        AtomicInteger threadCounter = new AtomicInteger(1);
        int threadCount = isMySQL ? 5 : 2;
        this.executor = Executors.newFixedThreadPool(threadCount, r -> {
            Thread thread = new Thread(r, "AtomTituls-DB-" + threadCounter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });

        setupDataSource(config);
    }

    private void setupDataSource(FileConfiguration config) {
        HikariConfig hikari = new HikariConfig();

        if (isMySQL) {
            String host = config.getString("mysql.host", "localhost");
            int port = config.getInt("mysql.port", 3306);
            String database = config.getString("mysql.database", "tituls");
            String user = config.getString("mysql.user", "root");
            String password = config.getString("mysql.password", "");
            int maxPoolSize = config.getInt("mysql.maximum-pool-size", 10);
            int minIdle = config.getInt("mysql.minimum-idle", 2);
            long connTimeout = config.getLong("mysql.connection-timeout", 5000);

            hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8");
            hikari.setUsername(user);
            hikari.setPassword(password);
            hikari.setMaximumPoolSize(maxPoolSize);
            hikari.setMinimumIdle(minIdle);
            hikari.setConnectionTimeout(connTimeout);

            hikari.addDataSourceProperty("cachePrepStmts", "true");
            hikari.addDataSourceProperty("prepStmtCacheSize", "250");
            hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikari.addDataSourceProperty("useServerPrepStmts", "true");
            hikari.addDataSourceProperty("rewriteBatchedStatements", "true");
            hikari.addDataSourceProperty("maintainTimeStats", "false");
        } else {
            String filePath = config.getString("sqlite.file", "database.db");
            File dbFile = new File(filePath);
            if (!dbFile.isAbsolute()) {
                dbFile = new File(plugin.getDataFolder(), filePath);
            }
            File parent = dbFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikari.setMaximumPoolSize(1);
            hikari.setConnectionTimeout(config.getLong("sqlite.busy-timeout", 10000));
            hikari.setConnectionInitSql("PRAGMA journal_mode = WAL; PRAGMA synchronous = NORMAL; PRAGMA foreign_keys = ON; PRAGMA busy_timeout = 10000;");
        }

        hikari.setPoolName("AtomTituls-Pool");
        this.dataSource = new HikariDataSource(hikari);
    }

    public CompletableFuture<Void> initializeTables() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("CREATE TABLE IF NOT EXISTS player_titles (" +
                        "uuid VARCHAR(36) NOT NULL, " +
                        "title_id VARCHAR(64) NOT NULL, " +
                        "title_type VARCHAR(16) NOT NULL, " +
                        "unlocked_at BIGINT, " +
                        "PRIMARY KEY (uuid, title_id))");

                stmt.execute("CREATE TABLE IF NOT EXISTS custom_titles (" +
                        "title_id VARCHAR(64) PRIMARY KEY, " +
                        "uuid VARCHAR(36) NOT NULL, " +
                        "display VARCHAR(128) NOT NULL, " +
                        "created_at BIGINT NOT NULL)");

                stmt.execute("CREATE TABLE IF NOT EXISTS player_active_title (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "title_id VARCHAR(64) NOT NULL, " +
                        "title_type VARCHAR(16) NOT NULL)");

                stmt.execute("CREATE TABLE IF NOT EXISTS player_settings (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "sort_mode VARCHAR(64))");

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize database tables", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public boolean isMySQL() {
        return isMySQL;
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
