package ru.atomicsqd.atomreactor.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.atomicsqd.atomreactor.AtomReactor;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Universal economy manager supporting Vault, PlayerPoints, CoinsEngine, and Internal storage.
 */
public class EconomyService {

    public enum Provider {
        VAULT,
        PLAYERPOINTS,
        COINSENGINE,
        INTERNAL
    }

    private final AtomReactor plugin;
    private Provider activeProvider = Provider.INTERNAL;

    // Vault
    private Economy vaultEconomy;

    // PlayerPoints reflection handle
    private Object playerPointsAPI;
    private Method ppGiveMethod;
    private Method ppTakeMethod;
    private Method ppLookMethod;

    // CoinsEngine reflection handle
    private Object coinsEngineCurrency;
    private Method ceAddBalanceMethod;
    private Method ceTakeBalanceMethod;
    private Method ceGetBalanceMethod;

    public EconomyService(AtomReactor plugin) {
        this.plugin = plugin;
    }

    public void init() {
        String configured = plugin.getConfig().getString("economy.provider", "VAULT").toUpperCase();
        try {
            this.activeProvider = Provider.valueOf(configured);
        } catch (IllegalArgumentException e) {
            this.activeProvider = Provider.VAULT;
        }

        switch (activeProvider) {
            case VAULT -> setupVault();
            case PLAYERPOINTS -> setupPlayerPoints();
            case COINSENGINE -> setupCoinsEngine();
            case INTERNAL -> plugin.getLogger().info("Using INTERNAL economy (reactor bank storage).");
        }
    }

    private void setupVault() {
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        if (vault == null || !vault.isEnabled()) {
            plugin.getLogger().warning("Vault not found! Falling back to INTERNAL economy.");
            activeProvider = Provider.INTERNAL;
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No Vault economy provider registered! Falling back to INTERNAL economy.");
            activeProvider = Provider.INTERNAL;
            return;
        }

        this.vaultEconomy = rsp.getProvider();
        plugin.getLogger().info("Successfully hooked into Vault Economy (" + vaultEconomy.getName() + ").");
    }

    private void setupPlayerPoints() {
        Plugin ppPlugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (ppPlugin == null || !ppPlugin.isEnabled()) {
            plugin.getLogger().warning("PlayerPoints not found! Falling back to INTERNAL economy.");
            activeProvider = Provider.INTERNAL;
            return;
        }

        try {
            Method getApiMethod = ppPlugin.getClass().getMethod("getAPI");
            this.playerPointsAPI = getApiMethod.invoke(ppPlugin);
            Class<?> apiClass = playerPointsAPI.getClass();

            this.ppGiveMethod = apiClass.getMethod("give", UUID.class, int.class);
            this.ppTakeMethod = apiClass.getMethod("take", UUID.class, int.class);
            this.ppLookMethod = apiClass.getMethod("look", UUID.class);

            plugin.getLogger().info("Successfully hooked into PlayerPoints API.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to hook PlayerPoints via reflection", e);
            activeProvider = Provider.INTERNAL;
        }
    }

    private void setupCoinsEngine() {
        Plugin cePlugin = Bukkit.getPluginManager().getPlugin("CoinsEngine");
        if (cePlugin == null || !cePlugin.isEnabled()) {
            plugin.getLogger().warning("CoinsEngine not found! Falling back to INTERNAL economy.");
            activeProvider = Provider.INTERNAL;
            return;
        }

        try {
            String currencyName = plugin.getConfig().getString("economy.coinsengine-currency", "coins");
            Class<?> apiClass = Class.forName("su.nightexpress.coinsengine.api.CoinsEngineAPI");
            Method getCurrencyMethod = apiClass.getMethod("getCurrency", String.class);
            this.coinsEngineCurrency = getCurrencyMethod.invoke(null, currencyName);

            if (this.coinsEngineCurrency == null) {
                plugin.getLogger().warning("CoinsEngine currency '" + currencyName + "' not found! Falling back to INTERNAL.");
                activeProvider = Provider.INTERNAL;
                return;
            }

            Class<?> currencyClass = coinsEngineCurrency.getClass();
            this.ceAddBalanceMethod = apiClass.getMethod("addBalance", Player.class, currencyClass, double.class);
            this.ceTakeBalanceMethod = apiClass.getMethod("removeBalance", Player.class, currencyClass, double.class);
            this.ceGetBalanceMethod = apiClass.getMethod("getBalance", Player.class, currencyClass);

            plugin.getLogger().info("Successfully hooked into CoinsEngine API with currency: " + currencyName);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to hook CoinsEngine API: " + e.getMessage() + ". Falling back to INTERNAL.");
            activeProvider = Provider.INTERNAL;
        }
    }

    public boolean deposit(Player player, double amount) {
        if (player == null || amount <= 0) return false;

        switch (activeProvider) {
            case VAULT -> {
                if (vaultEconomy != null) {
                    return vaultEconomy.depositPlayer(player, amount).transactionSuccess();
                }
            }
            case PLAYERPOINTS -> {
                if (playerPointsAPI != null && ppGiveMethod != null) {
                    try {
                        return (boolean) ppGiveMethod.invoke(playerPointsAPI, player.getUniqueId(), (int) Math.round(amount));
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to deposit PlayerPoints", e);
                    }
                }
            }
            case COINSENGINE -> {
                if (coinsEngineCurrency != null && ceAddBalanceMethod != null) {
                    try {
                        ceAddBalanceMethod.invoke(null, player, coinsEngineCurrency, amount);
                        return true;
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to deposit CoinsEngine", e);
                    }
                }
            }
            case INTERNAL -> {
                return true;
            }
        }
        return false;
    }

    public boolean has(Player player, double amount) {
        if (player == null) return false;
        if (amount <= 0) return true;

        switch (activeProvider) {
            case VAULT -> {
                if (vaultEconomy != null) {
                    return vaultEconomy.has(player, amount);
                }
            }
            case PLAYERPOINTS -> {
                if (playerPointsAPI != null && ppLookMethod != null) {
                    try {
                        int balance = (int) ppLookMethod.invoke(playerPointsAPI, player.getUniqueId());
                        return balance >= amount;
                    } catch (Exception ignored) {}
                }
            }
            case COINSENGINE -> {
                if (coinsEngineCurrency != null && ceGetBalanceMethod != null) {
                    try {
                        double balance = (double) ceGetBalanceMethod.invoke(null, player, coinsEngineCurrency);
                        return balance >= amount;
                    } catch (Exception ignored) {}
                }
            }
            case INTERNAL -> {
                return true;
            }
        }
        return false;
    }

    public boolean withdraw(Player player, double amount) {
        if (player == null || amount <= 0) return false;

        switch (activeProvider) {
            case VAULT -> {
                if (vaultEconomy != null) {
                    return vaultEconomy.withdrawPlayer(player, amount).transactionSuccess();
                }
            }
            case PLAYERPOINTS -> {
                if (playerPointsAPI != null && ppTakeMethod != null) {
                    try {
                        return (boolean) ppTakeMethod.invoke(playerPointsAPI, player.getUniqueId(), (int) Math.round(amount));
                    } catch (Exception ignored) {}
                }
            }
            case COINSENGINE -> {
                if (coinsEngineCurrency != null && ceTakeBalanceMethod != null) {
                    try {
                        ceTakeBalanceMethod.invoke(null, player, coinsEngineCurrency, amount);
                        return true;
                    } catch (Exception ignored) {}
                }
            }
            case INTERNAL -> {
                return true;
            }
        }
        return false;
    }

    public double getBalance(Player player) {
        if (player == null) return 0.0;

        switch (activeProvider) {
            case VAULT -> {
                if (vaultEconomy != null) {
                    return vaultEconomy.getBalance(player);
                }
            }
            case PLAYERPOINTS -> {
                if (playerPointsAPI != null && ppLookMethod != null) {
                    try {
                        return (int) ppLookMethod.invoke(playerPointsAPI, player.getUniqueId());
                    } catch (Exception ignored) {}
                }
            }
            case COINSENGINE -> {
                if (coinsEngineCurrency != null && ceGetBalanceMethod != null) {
                    try {
                        return (double) ceGetBalanceMethod.invoke(null, player, coinsEngineCurrency);
                    } catch (Exception ignored) {}
                }
            }
            case INTERNAL -> {
                return 0.0;
            }
        }
        return 0.0;
    }

    public Provider getActiveProvider() {
        return activeProvider;
    }
}
