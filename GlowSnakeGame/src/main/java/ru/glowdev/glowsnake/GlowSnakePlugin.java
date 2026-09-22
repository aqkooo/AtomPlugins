package ru.glowdev.glowsnake;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.glowdev.glowsnake.command.SnakeCommand;
import ru.glowdev.glowsnake.config.ConfigManager;
import ru.glowdev.glowsnake.game.GameManager;
import ru.glowdev.glowsnake.listener.InventoryListener;
import ru.glowdev.glowsnake.listener.PlayerMovementListener;
import ru.glowdev.glowsnake.listener.PlayerProtectionListener;
import ru.glowdev.glowsnake.storage.ScoreManager;

public class GlowSnakePlugin extends JavaPlugin {

    private static GlowSnakePlugin instance;

    private ConfigManager configManager;
    private ScoreManager scoreManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;

        // Load configs and storage
        this.configManager = new ConfigManager(this);
        this.scoreManager = new ScoreManager(this);
        this.gameManager = new GameManager(this);

        // Register listeners
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerMovementListener(this), this);
        pm.registerEvents(new InventoryListener(this), this);
        pm.registerEvents(new PlayerProtectionListener(this), this);

        // Register commands
        SnakeCommand snakeCommand = new SnakeCommand(this);
        PluginCommand cmd = getCommand("snake");
        if (cmd != null) {
            cmd.setExecutor(snakeCommand);
            cmd.setTabCompleter(snakeCommand);
        }

        getLogger().info("=========================================");
        getLogger().info(" GlowSnakeGame v" + getDescription().getVersion() + " by Ejyqyl, GlowDev");
        getLogger().info(" Successfully enabled!");
        getLogger().info("=========================================");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }

        if (scoreManager != null) {
            scoreManager.save();
        }

        getLogger().info("GlowSnakeGame disabled.");
        instance = null;
    }

    public static GlowSnakePlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
