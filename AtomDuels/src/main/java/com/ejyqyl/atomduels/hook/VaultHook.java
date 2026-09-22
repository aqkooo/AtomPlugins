package com.ejyqyl.atomduels.hook;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

/**
 * Safe Vault Economy hook with fallbacks when Vault is not installed or enabled.
 * Authored by ejyqyl.
 */
public class VaultHook {

    private final Plugin plugin;
    private Economy economy;

    public VaultHook(Plugin plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public double getBalance(UUID uuid) {
        if (!hasEconomy()) return 0.0;
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return economy.getBalance(player);
    }

    public boolean hasEnough(UUID uuid, double amount) {
        if (!hasEconomy() || amount <= 0) return true;
        return getBalance(uuid) >= amount;
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (!hasEconomy() || amount <= 0) return true;
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean deposit(UUID uuid, double amount) {
        if (!hasEconomy() || amount <= 0) return true;
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    public String format(double amount) {
        if (hasEconomy()) {
            return economy.format(amount);
        }
        return String.format("%.2f $", amount);
    }
}
