package ru.atomicsqd.atomreactor;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.atomicsqd.atomreactor.command.AtomReactorCommand;
import ru.atomicsqd.atomreactor.config.ConfigManager;
import ru.atomicsqd.atomreactor.economy.EconomyService;
import ru.atomicsqd.atomreactor.listener.GUIListener;
import ru.atomicsqd.atomreactor.listener.ReactorBlockListener;
import ru.atomicsqd.atomreactor.model.Reactor;
import ru.atomicsqd.atomreactor.service.HologramService;
import ru.atomicsqd.atomreactor.service.ReactorManager;
import ru.atomicsqd.atomreactor.service.ReactorTicker;

/**
 * AtomReactor - Upgradeable reactor block generating Vault, PlayerPoints, or CoinsEngine currency.
 *
 * Authors: ejyqyl, atomicsqd
 * Telegram: https://t.me/atomicsqd
 */
public final class AtomReactor extends JavaPlugin {

    private static AtomReactor instance;

    private ConfigManager configManager;
    private EconomyService economyService;
    private HologramService hologramService;
    private ReactorManager reactorManager;
    private ReactorTicker reactorTicker;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Config & Economy
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.economyService = new EconomyService(this);
        this.economyService.init();

        // 2. Services
        this.hologramService = new HologramService(this);
        this.reactorManager = new ReactorManager(this, hologramService);
        this.reactorManager.loadReactors();

        this.reactorTicker = new ReactorTicker(this, reactorManager, hologramService, economyService);
        this.reactorTicker.start();

        // 3. Register Listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ReactorBlockListener(this, reactorManager), this);
        pm.registerEvents(new GUIListener(this, reactorManager, hologramService, economyService), this);

        // 4. Register Command
        AtomReactorCommand cmd = new AtomReactorCommand(this, reactorManager, hologramService, reactorTicker);
        var pluginCommand = getCommand("atomreactor");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(cmd);
            pluginCommand.setTabCompleter(cmd);
        }

        // 5. Initial Holograms
        for (Reactor r : reactorManager.getAllReactors()) {
            hologramService.updateHologram(r);
        }

        // 6. Console Banner
        getLogger().info("==========================================");
        getLogger().info("  AtomReactor v" + getDescription().getVersion() + " successfully enabled!");
        getLogger().info("  Authors: ejyqyl, atomicsqd");
        getLogger().info("  Active Economy: " + economyService.getActiveProvider());
        getLogger().info("  Reactor Block: " + configManager.getReactorMaterial());
        getLogger().info("==========================================");
    }

    @Override
    public void onDisable() {
        if (reactorTicker != null) {
            reactorTicker.stop();
        }
        if (reactorManager != null) {
            for (Reactor r : reactorManager.getAllReactors()) {
                if (hologramService != null) {
                    hologramService.removeHologram(r);
                }
            }
            reactorManager.saveReactors();
        }
        getLogger().info("AtomReactor successfully disabled.");
    }

    public static AtomReactor getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public HologramService getHologramService() {
        return hologramService;
    }

    public ReactorManager getReactorManager() {
        return reactorManager;
    }

    public ReactorTicker getReactorTicker() {
        return reactorTicker;
    }
}
