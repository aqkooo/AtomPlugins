package com.ejyqyl.atompvpbot;

import com.ejyqyl.atompvpbot.arena.ArenaManager;
import com.ejyqyl.atompvpbot.command.PvPBotCommand;
import com.ejyqyl.atompvpbot.config.ConfigManager;
import com.ejyqyl.atompvpbot.config.MessageManager;
import com.ejyqyl.atompvpbot.data.DatabaseManager;
import com.ejyqyl.atompvpbot.data.StatsManager;
import com.ejyqyl.atompvpbot.fight.FightManager;
import com.ejyqyl.atompvpbot.hook.BotPlaceholderExpansion;
import com.ejyqyl.atompvpbot.kit.KitManager;
import com.ejyqyl.atompvpbot.listener.ArenaProtectionListener;
import com.ejyqyl.atompvpbot.listener.BotCombatListener;
import com.ejyqyl.atompvpbot.listener.MenuListener;
import com.ejyqyl.atompvpbot.listener.PlayerConnectionListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * AtomPvPbot - High-fidelity PvP bot trainer plugin for Paper 1.21+.
 *
 * @author ejyqyl (aqkooo - https://github.com/aqkooo)
 */
public final class AtomPvPbot extends JavaPlugin {

    private static AtomPvPbot instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private StatsManager statsManager;
    private ArenaManager arenaManager;
    private KitManager kitManager;
    private FightManager fightManager;

    @Override
    public void onEnable() {
        instance = this;

        // Print Startup Banner
        logBanner();

        // 1. Core Managers
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.statsManager = new StatsManager(databaseManager);
        this.arenaManager = new ArenaManager(this);
        this.kitManager = new KitManager(this);

        arenaManager.loadArenas();
        kitManager.loadKits();

        // 2. Fight Manager
        this.fightManager = new FightManager(this, statsManager);

        // 3. Register Events
        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
        Bukkit.getPluginManager().registerEvents(new BotCombatListener(fightManager), this);
        Bukkit.getPluginManager().registerEvents(new ArenaProtectionListener(fightManager, arenaManager, configManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(fightManager, statsManager), this);

        // 4. Register Commands
        PvPBotCommand cmd = new PvPBotCommand(this, arenaManager, kitManager, fightManager, statsManager, configManager, messageManager);
        PluginCommand pvpbotCmd = getCommand("pvpbot");
        if (pvpbotCmd != null) {
            pvpbotCmd.setExecutor(cmd);
            pvpbotCmd.setTabCompleter(cmd);
        }

        // 5. PlaceholderAPI Hook
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new BotPlaceholderExpansion(this, statsManager, fightManager).register();
            getLogger().info("Successfully hooked into PlaceholderAPI.");
        }

        getLogger().info("AtomPvPbot v" + getDescription().getVersion() + " successfully enabled! Author: ejyqyl (https://github.com/aqkooo)");
    }

    @Override
    public void onDisable() {
        if (fightManager != null) {
            fightManager.stopAllFights();
        }
        if (statsManager != null) {
            statsManager.saveAll();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        if (arenaManager != null) {
            arenaManager.saveArenas();
        }
        if (kitManager != null) {
            kitManager.saveKits();
        }

        getLogger().info("AtomPvPbot has been safely disabled.");
        instance = null;
    }

    private void logBanner() {
        getLogger().info("==================================================");
        getLogger().info("    ___  _                    ______     ______  _   ");
        getLogger().info("   / _ \\| |                   | ___ \\    | ___ \\| |  ");
        getLogger().info("  / /_\\ \\ |_ ___  _ __ ___    | |_/ /   | |_/ /| |_ ");
        getLogger().info("  |  _  | __/ _ \\| '_ ` _ \\   |  __/    | ___ \\| __|");
        getLogger().info("  | | | | || (_) | | | | | |  | |       | |_/ /| |_ ");
        getLogger().info("  \\_| |_/\\__\\___/|_| |_| |_|  \\_|       \\____/ \\___|");
        getLogger().info("           AtomPvPbot - Advanced Bot Training       ");
        getLogger().info("    Author: ejyqyl (GitHub: https://github.com/aqkooo) ");
        getLogger().info("==================================================");
    }

    public static AtomPvPbot getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public KitManager getKitManager() { return kitManager; }
    public FightManager getFightManager() { return fightManager; }
}
