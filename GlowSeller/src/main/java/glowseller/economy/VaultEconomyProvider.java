package glowseller.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomyProvider implements EconomyProvider {
    private Economy economy;

    public VaultEconomyProvider() {
        hook();
    }

    private void hook() {
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                this.economy = rsp.getProvider();
            }
        }
    }

    @Override
    public String getName() {
        return "Vault";
    }

    @Override
    public boolean isAvailable() {
        if (economy == null) {
            hook();
        }
        return economy != null;
    }

    @Override
    public double getBalance(Player player) {
        if (!isAvailable() || player == null) return 0.0;
        return economy.getBalance(player);
    }

    @Override
    public boolean deposit(Player player, double amount) {
        if (!isAvailable() || player == null || amount < 0) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (!isAvailable() || player == null || amount < 0) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
}
