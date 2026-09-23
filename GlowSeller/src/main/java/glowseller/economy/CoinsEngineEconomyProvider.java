package glowseller.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;

public class CoinsEngineEconomyProvider implements EconomyProvider {
    private boolean available = false;
    private Method getBalanceMethod;
    private Method addBalanceMethod;
    private Method removeBalanceMethod;
    private String currency = "coins";

    public CoinsEngineEconomyProvider() {
        hook();
    }

    private void hook() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CoinsEngine");
        if (plugin != null && plugin.isEnabled()) {
            try {
                Class<?> apiClass = Class.forName("su.nightexpress.coinsengine.api.CoinsEngineAPI");
                for (Method m : apiClass.getMethods()) {
                    if (m.getName().equals("getBalance") && m.getParameterCount() >= 1) {
                        getBalanceMethod = m;
                    } else if ((m.getName().equals("addBalance") || m.getName().equals("deposit")) && m.getParameterCount() >= 2) {
                        addBalanceMethod = m;
                    } else if ((m.getName().equals("removeBalance") || m.getName().equals("withdraw")) && m.getParameterCount() >= 2) {
                        removeBalanceMethod = m;
                    }
                }
                available = true;
            } catch (Exception e) {
                available = false;
            }
        }
    }

    @Override
    public String getName() {
        return "CoinsEngine";
    }

    @Override
    public boolean isAvailable() {
        if (!available) hook();
        return available;
    }

    @Override
    public double getBalance(Player player) {
        if (!isAvailable() || player == null || getBalanceMethod == null) return 0.0;
        try {
            Object res;
            if (getBalanceMethod.getParameterCount() == 1) {
                res = getBalanceMethod.invoke(null, player);
            } else {
                res = getBalanceMethod.invoke(null, player, currency);
            }
            if (res instanceof Number n) {
                return n.doubleValue();
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "Failed to get balance from CoinsEngine", e);
        }
        return 0.0;
    }

    @Override
    public boolean deposit(Player player, double amount) {
        if (!isAvailable() || player == null || addBalanceMethod == null) return false;
        try {
            if (addBalanceMethod.getParameterCount() == 2) {
                addBalanceMethod.invoke(null, player, amount);
            } else {
                addBalanceMethod.invoke(null, player, currency, amount);
            }
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "Failed to deposit to CoinsEngine", e);
            return false;
        }
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (!isAvailable() || player == null || removeBalanceMethod == null) return false;
        try {
            if (removeBalanceMethod.getParameterCount() == 2) {
                removeBalanceMethod.invoke(null, player, amount);
            } else {
                removeBalanceMethod.invoke(null, player, currency, amount);
            }
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "Failed to withdraw from CoinsEngine", e);
            return false;
        }
    }
}
