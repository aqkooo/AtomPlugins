package ru.atomicsqd.atommessage;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.atomicsqd.atommessage.command.AtomMessageCommand;
import ru.atomicsqd.atommessage.config.ConfigManager;
import ru.atomicsqd.atommessage.listener.PlayerListener;
import ru.atomicsqd.atommessage.manager.ChatAnnouncementManager;
import ru.atomicsqd.atommessage.manager.TabMessageManager;
import ru.atomicsqd.atommessage.placeholder.AtomMessageExpansion;

public final class AtomMessage extends JavaPlugin {
    private static AtomMessage instance;

    private ConfigManager configManager;
    private TabMessageManager tabMessageManager;
    private ChatAnnouncementManager chatAnnouncementManager;

    public static AtomMessage getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("=========================================");
        getLogger().info(" AtomMessage v" + getDescription().getVersion() + " by " + String.join(", ", getDescription().getAuthors()));
        getLogger().info(" Interactive chat broadcasts & TAB engine");
        getLogger().info(" Paper / Purpur 1.20-1.21.x with MiniMessage");
        getLogger().info("=========================================");

        // 1. Load configurations
        configManager = new ConfigManager(this);
        configManager.load();

        // 2. Initialize and start managers
        tabMessageManager = new TabMessageManager(this, configManager);
        tabMessageManager.start();

        chatAnnouncementManager = new ChatAnnouncementManager(this, configManager);
        chatAnnouncementManager.start();

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
            getLogger().info("Registered PlaceholderAPI expansion: %atommessage_tab%, %atommessage_chat%, etc.");
        }

        getLogger().info("AtomMessage enabled successfully!");
    }

    public void reload() {
        configManager.load();
        tabMessageManager.start();
        chatAnnouncementManager.start();
        getLogger().info("AtomMessage reloaded successfully!");
    }

    @Override
    public void onDisable() {
        if (tabMessageManager != null) {
            tabMessageManager.stop();
        }
        if (chatAnnouncementManager != null) {
            chatAnnouncementManager.stop();
        }
        getLogger().info("AtomMessage disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TabMessageManager getTabMessageManager() {
        return tabMessageManager;
    }

    public ChatAnnouncementManager getChatAnnouncementManager() {
        return chatAnnouncementManager;
    }
}
