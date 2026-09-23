package glowseller;

import glowseller.cache.PlayerDataCache;
import glowseller.commands.GlowSellerAdminCommand;
import glowseller.commands.SellCommand;
import glowseller.commands.ShopCommand;
import glowseller.configs.impl.*;
import glowseller.database.DataBaseManager;
import glowseller.database.repositories.PlayerRepository;
import glowseller.database.repositories.PurchaseLogRepository;
import glowseller.economy.*;
import glowseller.listeners.MenuListener;
import glowseller.listeners.PlayerListener;
import glowseller.managers.*;
import glowseller.menu.MenuManager;
import glowseller.models.PlayerData;
import glowseller.tasks.AutoSellTask;
import glowseller.tasks.BoosterExpireTask;
import glowseller.tasks.SavePlayerDataCacheTask;
import glowseller.utils.PlaceholderHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class Main extends JavaPlugin {
    private static Main instance;

    // Configs
    private MainConfig mainConfig;
    private ItemsConfig itemsConfig;
    private ShopConfig shopConfig;
    private MessageConfig messageConfig;
    private DataBaseConfig dataBaseConfig;

    // Database & Repositories
    private DataBaseManager dataBaseManager;
    private PlayerRepository playerRepository;
    private PurchaseLogRepository purchaseLogRepository;

    // Cache
    private PlayerDataCache playerDataCache;

    // Economy
    private EconomyProvider economyProvider;

    // Managers
    private SellManager sellManager;
    private ItemManager itemManager;
    private ShopManager shopManager;
    private BoosterManager boosterManager;
    private NumberFormatManager numberFormatManager;
    private MenuManager menuManager;

    // Tasks
    private SavePlayerDataCacheTask saveTask;
    private BoosterExpireTask boosterExpireTask;
    private AutoSellTask autoSellTask;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Load Configurations
        initConfigs();

        // 2. Database
        dataBaseManager = new DataBaseManager(this);
        playerRepository = new PlayerRepository(dataBaseManager, getLogger());
        purchaseLogRepository = new PurchaseLogRepository(dataBaseManager, getLogger());

        // 3. Cache
        playerDataCache = new PlayerDataCache();

        // 4. Economy Provider
        setupEconomy();

        // 5. Managers
        numberFormatManager = new NumberFormatManager();
        boosterManager = new BoosterManager(this);
        itemManager = new ItemManager(this);
        sellManager = new SellManager(this);
        shopManager = new ShopManager(this);
        menuManager = new MenuManager(this);

        // 6. Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        // 7. Commands
        SellCommand sellCmd = new SellCommand(this);
        if (getCommand("sell") != null) {
            getCommand("sell").setExecutor(sellCmd);
            getCommand("sell").setTabCompleter(sellCmd);
        }

        ShopCommand shopCmd = new ShopCommand(this);
        if (getCommand("shop") != null) {
            getCommand("shop").setExecutor(shopCmd);
        }

        GlowSellerAdminCommand adminCmd = new GlowSellerAdminCommand(this);
        if (getCommand("glowseller") != null) {
            getCommand("glowseller").setExecutor(adminCmd);
            getCommand("glowseller").setTabCompleter(adminCmd);
        }

        // 8. PlaceholderAPI Hook
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI expansion successfully hooked!");
        }

        // 9. Load Online Players (for reload scenario)
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData data = playerRepository.load(p.getUniqueId());
            playerDataCache.put(p.getUniqueId(), data);
        }

        // 10. Start Tasks
        long intervalTicks = Math.max(30L, mainConfig.getSaveIntervalSeconds()) * 20L;
        saveTask = new SavePlayerDataCacheTask(this);
        saveTask.runTaskTimerAsynchronously(this, intervalTicks, intervalTicks);

        boosterExpireTask = new BoosterExpireTask(this);
        boosterExpireTask.runTaskTimer(this, 20L, 20L);

        autoSellTask = new AutoSellTask(this);
        autoSellTask.runTaskTimer(this, 100L, 100L);

        getLogger().info("GlowSeller v" + getDescription().getVersion() + " has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        // 1. Cancel tasks
        getServer().getScheduler().cancelTasks(this);

        // 2. Close menus
        if (menuManager != null) {
            menuManager.closeAll();
        }

        // 3. Save player cache synchronously
        if (playerRepository != null && playerDataCache != null) {
            playerRepository.saveAll(playerDataCache.getAll());
        }

        // 4. Close database pool
        if (dataBaseManager != null) {
            dataBaseManager.close();
        }

        getLogger().info("GlowSeller has been disabled!");
    }

    private void initConfigs() {
        dataBaseConfig = new DataBaseConfig(this);
        mainConfig = new MainConfig(this);
        messageConfig = new MessageConfig(this);
        itemsConfig = new ItemsConfig(this);
        shopConfig = new ShopConfig(this);
    }

    public void reloadAll() {
        mainConfig.reload();
        messageConfig.reload();
        itemsConfig.reload();
        shopConfig.reload();
        if (itemManager != null) {
            itemManager.rebuildIndex();
        }
        setupEconomy();
    }

    private void setupEconomy() {
        String type = mainConfig != null ? mainConfig.getEconomyType() : "VAULT";
        switch (type) {
            case "COINSENGINE" -> economyProvider = new CoinsEngineEconomyProvider();
            case "PLAYERPOINTS" -> economyProvider = new PlayerPointsEconomyProvider();
            default -> economyProvider = new VaultEconomyProvider();
        }

        if (economyProvider.isAvailable()) {
            getLogger().info("Economy hooked: " + economyProvider.getName());
        } else {
            getLogger().log(Level.WARNING, "Economy provider '" + type + "' not available yet. Make sure your economy plugin is installed.");
        }
    }

    public static Main getInstance() {
        return instance;
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

    public ItemsConfig getItemsConfig() {
        return itemsConfig;
    }

    public ShopConfig getShopConfig() {
        return shopConfig;
    }

    public MessageConfig getMessageConfig() {
        return messageConfig;
    }

    public DataBaseConfig getDataBaseConfig() {
        return dataBaseConfig;
    }

    public DataBaseManager getDataBaseManager() {
        return dataBaseManager;
    }

    public PlayerRepository getPlayerRepository() {
        return playerRepository;
    }

    public PurchaseLogRepository getPurchaseLogRepository() {
        return purchaseLogRepository;
    }

    public PlayerDataCache getPlayerDataCache() {
        return playerDataCache;
    }

    public EconomyProvider getEconomyProvider() {
        return economyProvider;
    }

    public SellManager getSellManager() {
        return sellManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public BoosterManager getBoosterManager() {
        return boosterManager;
    }

    public NumberFormatManager getNumberFormatManager() {
        return numberFormatManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }
}
