package ru.glowdevv.glowcustomloot;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import ru.glowdevv.glowcustomloot.command.EditLootCommand;
import ru.glowdevv.glowcustomloot.command.GclCommand;
import ru.glowdevv.glowcustomloot.config.ConfigManager;
import ru.glowdevv.glowcustomloot.config.LootTableManager;
import ru.glowdevv.glowcustomloot.listener.LootListener;
import ru.glowdevv.glowcustomloot.listener.MenuListener;
import ru.glowdevv.glowcustomloot.provider.ItemProviderRegistry;
import ru.glowdevv.glowcustomloot.util.FoliaScheduler;

public class GlowCustomLootPlugin extends JavaPlugin {
    private static GlowCustomLootPlugin instance;

    private ConfigManager configManager;
    private ItemProviderRegistry itemProviderRegistry;
    private LootTableManager lootTableManager;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);
        this.configManager.loadAll();

        this.itemProviderRegistry = new ItemProviderRegistry(this);
        this.itemProviderRegistry.registerDefaults();

        this.lootTableManager = new LootTableManager(this, itemProviderRegistry);
        this.lootTableManager.loadAll();

        // Register listeners
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new LootListener(this), this);

        // Register commands
        PluginCommand editLootCmd = getCommand("editloot");
        if (editLootCmd != null) {
            editLootCmd.setExecutor(new EditLootCommand(this));
        }

        PluginCommand gclCmd = getCommand("gcl");
        if (gclCmd != null) {
            GclCommand cmd = new GclCommand(this);
            gclCmd.setExecutor(cmd);
            gclCmd.setTabCompleter(cmd);
        }

        getLogger().info("=========================================");
        getLogger().info(" GlowCustomLoot v" + getDescription().getVersion());
        getLogger().info(" Authors: ejyqyl, glowdevv (t.me/glowdevv)");
        getLogger().info(" Folia supported: " + FoliaScheduler.isFolia());
        getLogger().info(" Successfully enabled!");
        getLogger().info("=========================================");
    }

    @Override
    public void onDisable() {
        if (lootTableManager != null) {
            lootTableManager.saveAllDirtySync();
        }

        getLogger().info("GlowCustomLoot disabled. Saved all pending changes.");
        instance = null;
    }

    public static GlowCustomLootPlugin getInstance() {
        return instance;
    }

    @NotNull
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @NotNull
    public ItemProviderRegistry getItemProviderRegistry() {
        return itemProviderRegistry;
    }

    @NotNull
    public LootTableManager getLootTableManager() {
        return lootTableManager;
    }
}
