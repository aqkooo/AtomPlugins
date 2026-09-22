package ru.atomicsqd.atommessage;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.atomicsqd.atommessage.command.AtomMessageCommand;
import ru.atomicsqd.atommessage.config.ConfigManager;
import ru.atomicsqd.atommessage.listener.PlayerListener;
import ru.atomicsqd.atommessage.manager.TabMessageManager;
import ru.atomicsqd.atommessage.placeholder.AtomMessageExpansion;

public final class AtomMessage extends JavaPlugin {
    private static AtomMessage instance;

    private ConfigManager configManager;
    private TabMessageManager tabMessageManager;

    public static AtomMessage getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("=========================================");
        getLogger().info(" AtomMessage v" + getDescription().getVersion() + " by " + String.join(", ", getDescription().getAuthors()));
        getLogger().info(" Auto-messages in TAB for Paper 1.20-1.21.x");
        getLogger().info("=========================================");

        // 1. Load configurations
        configManager = new ConfigManager(this);
        configManager.load();

        // 2. Initialize and start rotation manager
        tabMessageManager = new TabMessageManager(this, configManager);
        tabMessageManager.start();

        // 3. Register commands
        AtomMessageCommand command = new AtomMessageCommand(this);
        PluginCommand cmd = getCommand("atommessage");
        if (cmd != null) {
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        }

        // 4. Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // 5. Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new AtomMessageExpansion(this).register();
            getLogger().info("Registered PlaceholderAPI expansion: %atommessage_tab%, %atommessage_message%, etc.");
        }

        getLogger().info("AtomMessage enabled successfully!");
    }

    public void reload() {
        configManager.load();
        tabMessageManager.start();
        getLogger().info("AtomMessage reloaded successfully!");
    }

    @Override
    public void onDisable() {
        if (tabMessageManager != null) {
            tabMessageManager.stop();
        }
        getLogger().info("AtomMessage disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TabMessageManager getTabMessageManager() {
        return tabMessageManager;
    }
}
