package com.ejyqyl.glowchatgame;

import com.ejyqyl.glowchatgame.command.ChatGameCommand;
import com.ejyqyl.glowchatgame.config.ConfigManager;
import com.ejyqyl.glowchatgame.config.MessageManager;
import com.ejyqyl.glowchatgame.data.DatabaseManager;
import com.ejyqyl.glowchatgame.data.StatsManager;
import com.ejyqyl.glowchatgame.game.GameManager;
import com.ejyqyl.glowchatgame.hook.PlaceholderHook;
import com.ejyqyl.glowchatgame.listener.ChatListener;
import com.ejyqyl.glowchatgame.listener.PlayerListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * GlowChatGame - Ultra-optimized, highly configurable math chat mini-game.
 *
 * @author ejyqyl, Glowdevv
 */
public final class GlowChatGame extends JavaPlugin {

    private static GlowChatGame instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private StatsManager statsManager;
    private GameManager gameManager;
    private PlaceholderHook placeholderHook;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Config and Localization
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);

        // 2. Storage & Statistics
        this.databaseManager = new DatabaseManager(this, configManager.getDatabaseFile());
        this.statsManager = new StatsManager(this, databaseManager);

        // 3. Game Engine
        this.gameManager = new GameManager(this);

        // 4. Commands & Listeners
        registerCommands();
        registerListeners();

        // 5. Hooks
        this.placeholderHook = new PlaceholderHook(this);
        this.placeholderHook.register();

        getLogger().info("GlowChatGame v" + getDescription().getVersion() + " by ejyqyl & Glowdevv successfully enabled!");
    }

    @Override
    public void onDisable() {
        // Unregister PAPI
        if (placeholderHook != null) {
            placeholderHook.unregister();
        }

        // Cancel running games and timers
        if (gameManager != null) {
            gameManager.cancelScheduler();
            if (gameManager.isGameRunning()) {
                gameManager.stopCurrentGame();
            }
        }

        // Flush stats and close database
        if (statsManager != null) {
            statsManager.saveAll();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }

        getServer().getScheduler().cancelTasks(this);
        getLogger().info("GlowChatGame disabled successfully.");
        instance = null;
    }

    public synchronized void reloadAll() {
        if (configManager != null) configManager.reload();
        if (messageManager != null) messageManager.reload();
        if (gameManager != null) gameManager.reload();
        getLogger().info("GlowChatGame reloaded successfully.");
    }

    private void registerCommands() {
        PluginCommand cmd = getCommand("chatgame");
        if (cmd != null) {
            ChatGameCommand handler = new ChatGameCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
    }

    public static GlowChatGame getInstance() {
        return instance;
    }

    @NotNull
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @NotNull
    public MessageManager getMessageManager() {
        return messageManager;
    }

    @NotNull
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @NotNull
    public StatsManager getStatsManager() {
        return statsManager;
    }

    @NotNull
    public GameManager getGameManager() {
        return gameManager;
    }

    public PlaceholderHook getPlaceholderHook() {
        return placeholderHook;
    }
}
