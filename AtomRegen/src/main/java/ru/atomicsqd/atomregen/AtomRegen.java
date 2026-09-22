package ru.atomicsqd.atomregen;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.atomicsqd.atomregen.command.AtomRegenCommand;
import ru.atomicsqd.atomregen.listener.BlockListener;
import ru.atomicsqd.atomregen.listener.ExplosionListener;
import ru.atomicsqd.atomregen.listener.SelectionListener;
import ru.atomicsqd.atomregen.service.RegenEngine;
import ru.atomicsqd.atomregen.service.RegionManager;
import ru.atomicsqd.atomregen.service.SelectionService;

/**
 * AtomRegen - Dynamic snapshot and auto-regenerating regions for Paper 1.21.x
 *
 * Authors: ejyqyl, atomicsqd
 * Telegram: https://t.me/atomicsqd
 */
public final class AtomRegen extends JavaPlugin {

    private static AtomRegen instance;

    private SelectionService selectionService;
    private RegionManager regionManager;
    private RegenEngine regenEngine;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Config initialization
        saveDefaultConfig();

        // 2. Services initialization
        this.selectionService = new SelectionService(this);
        this.regionManager = new RegionManager(this);
        this.regionManager.loadRegions();

        this.regenEngine = new RegenEngine(this, regionManager);
        this.regenEngine.start();

        // 3. Register Event Listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new SelectionListener(this, selectionService), this);
        pm.registerEvents(new BlockListener(this, regionManager, regenEngine), this);
        pm.registerEvents(new ExplosionListener(this, regionManager, regenEngine), this);

        // 4. Register Commands & Tab Completer
        AtomRegenCommand cmd = new AtomRegenCommand(this, selectionService, regionManager, regenEngine);
        var pluginCommand = getCommand("atomregen");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(cmd);
            pluginCommand.setTabCompleter(cmd);
        }

        // 5. Console banner
        getLogger().info("==========================================");
        getLogger().info("  AtomRegen v" + getDescription().getVersion() + " successfully enabled!");
        getLogger().info("  Authors: ejyqyl, atomicsqd");
        getLogger().info("  Paper 1.21.x | Java 21");
        getLogger().info("==========================================");
    }

    @Override
    public void onDisable() {
        if (regenEngine != null) {
            regenEngine.stop();
        }
        getLogger().info("AtomRegen successfully disabled.");
    }

    public static AtomRegen getInstance() {
        return instance;
    }

    public SelectionService getSelectionService() {
        return selectionService;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public RegenEngine getRegenEngine() {
        return regenEngine;
    }
}
