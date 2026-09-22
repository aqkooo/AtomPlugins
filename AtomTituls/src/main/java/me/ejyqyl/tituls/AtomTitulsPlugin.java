package me.ejyqyl.tituls;

import me.ejyqyl.tituls.command.TitulsCommand;
import me.ejyqyl.tituls.listener.MenuListener;
import me.ejyqyl.tituls.listener.PlayerListener;
import me.ejyqyl.tituls.listener.TagInteractListener;
import me.ejyqyl.tituls.manager.ConfigManager;
import me.ejyqyl.tituls.manager.PlayerCache;
import me.ejyqyl.tituls.manager.TitulManager;
import me.ejyqyl.tituls.manager.TitulTagManager;
import me.ejyqyl.tituls.placeholder.TitulsPlaceholderExpansion;
import me.ejyqyl.tituls.storage.DatabaseManager;
import me.ejyqyl.tituls.storage.TitleStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class AtomTitulsPlugin extends JavaPlugin {
    private static AtomTitulsPlugin instance;

    private ConfigManager configManager;
    private TitulManager titulManager;
    private DatabaseManager databaseManager;
    private TitleStorage titleStorage;
    private PlayerCache playerCache;
    private TitulTagManager titulTagManager;

    public static AtomTitulsPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("=========================================");
        getLogger().info(" AtomTituls v" + getDescription().getVersion() + " by " + getDescription().getAuthors());
        getLogger().info(" Rewritten for Paper 1.20-1.21.x");
        getLogger().info("=========================================");

        // 1. Configs
        configManager = new ConfigManager(this);
        configManager.loadAll();

        // 2. Tituls registry
        titulManager = new TitulManager();
        titulManager.loadFromConfig(configManager.getTitulsConfig().getConfigurationSection("tituls"));
        getLogger().info("Loaded " + titulManager.getCount() + " standard case titles from tituls.yml");

        // 3. Database & Storage
        try {
            databaseManager = new DatabaseManager(this, configManager.getDatabaseConfig());
            databaseManager.initializeTables().join();
            getLogger().info("Database connected & tables verified (" + (databaseManager.isMySQL() ? "MySQL" : "SQLite") + ")");
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Failed to connect to database! Disabling plugin...", ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        titleStorage = new TitleStorage(databaseManager);
        playerCache = new PlayerCache(this, titleStorage);
        titulTagManager = new TitulTagManager(this);

        // 4. Listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new MenuListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new TagInteractListener(this), this);

        // 5. Commands
        TitulsCommand titulsCmd = new TitulsCommand(this);
        registerCommand("tituls", titulsCmd);
        registerCommand("atomtituls", titulsCmd);

        // 6. Online players cache warm-up
        for (Player online : Bukkit.getOnlinePlayers()) {
            playerCache.load(online.getUniqueId());
        }

        // 7. PlaceholderAPI hooks
        if (pm.getPlugin("PlaceholderAPI") != null) {
            new TitulsPlaceholderExpansion(this, "stickhwtituls").register();
            new TitulsPlaceholderExpansion(this, "tituls").register();
            new TitulsPlaceholderExpansion(this, "atomtituls").register();
            getLogger().info("Registered PlaceholderAPI expansions: %stickhwtituls_*%, %tituls_*%, %atomtituls_*%");
        }

        getLogger().info("AtomTituls enabled successfully!");
    }

    private void registerCommand(String name, TitulsCommand command) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        }
    }

    public void reloadAll() {
        configManager.loadAll();
        titulManager.loadFromConfig(configManager.getTitulsConfig().getConfigurationSection("tituls"));
        getLogger().info("Reloaded configs and " + titulManager.getCount() + " titles.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
            getLogger().info("Database pool closed safely.");
        }
        getLogger().info("AtomTituls disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TitulManager getTitulManager() {
        return titulManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TitleStorage getTitleStorage() {
        return titleStorage;
    }

    public PlayerCache getPlayerCache() {
        return playerCache;
    }

    public TitulTagManager getTitulTagManager() {
        return titulTagManager;
    }
}
