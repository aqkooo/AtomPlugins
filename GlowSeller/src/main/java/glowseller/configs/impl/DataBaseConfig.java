package glowseller.configs.impl;

import glowseller.Main;
import glowseller.configs.CustomConfig;

public class DataBaseConfig extends CustomConfig {
    public enum Type {
        SQLITE,
        MYSQL,
        POSTGRESQL
    }

    private Type type = Type.SQLITE;
    private String host = "localhost";
    private int port = 3306;
    private String database = "glowseller";
    private String username = "root";
    private String password = "";
    private int poolSize = 10;
    private long connectionTimeout = 30000L;
    private String sqliteFileName = "database.db";

    public DataBaseConfig(Main plugin) {
        super(plugin, "database.yml");
        reload();
    }

    @Override
    public void parse() {
        String typeStr = config.getString("type", "SQLITE").toUpperCase();
        try {
            type = Type.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            type = Type.SQLITE;
        }

        host = config.getString("mysql.host", "localhost");
        port = config.getInt("mysql.port", type == Type.POSTGRESQL ? 5432 : 3306);
        database = config.getString("mysql.database", "glowseller");
        username = config.getString("mysql.username", "root");
        password = config.getString("mysql.password", "");
        poolSize = config.getInt("pool.maximum_pool_size", 10);
        connectionTimeout = config.getLong("pool.connection_timeout", 30000L);
        sqliteFileName = config.getString("sqlite.file", "database.db");
    }

    public Type getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public String getSqliteFileName() {
        return sqliteFileName;
    }
}
