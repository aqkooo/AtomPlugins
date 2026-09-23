package glowseller.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerPointsEconomyProvider implements EconomyProvider {
    private boolean available = false;
    private Object pointsApi;
    private Method lookMethod;
    private Method giveMethod;
    private Method takeMethod;

    public PlayerPointsEconomyProvider() {
        hook();
    }

    private void hook() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (plugin != null && plugin.isEnabled()) {
            try {
                Class<?> ppClass = Class.forName("org.black_ixx.playerpoints.PlayerPoints");
                Method getApiMethod = ppClass.getMethod("getAPI");
                pointsApi = getApiMethod.invoke(plugin);
                if (pointsApi != null) {
                    Class<?> apiClass = pointsApi.getClass();
                    lookMethod = apiClass.getMethod("look", UUID.class);
                    giveMethod = apiClass.getMethod("give", UUID.class, int.class);
                    takeMethod = apiClass.getMethod("take", UUID.class, int.class);
                    available = true;
                }
            } catch (Exception e) {
                available = false;
            }
        }
    }

    @Override
    public String getName() {
        return "PlayerPoints";
    }

    @Override
    public boolean isAvailable() {
        if (!available) hook();
        return available;
    }

    @Override
    public double getBalance(Player player) {
        if (!isAvailable() || player == null || lookMethod == null) return 0.0;
        try {
            Object res = lookMethod.invoke(pointsApi, player.getUniqueId());
            if (res instanceof Number n) {
                return n.doubleValue();
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "Failed to get balance from PlayerPoints", e);
        }
        return 0.0;
    }

    @Override
    public boolean deposit(Player player, double amount) {
        if (!isAvailable() || player == null || giveMethod == null || amount < 0) return false;
        try {
            Object res = giveMethod.invoke(pointsApi, player.getUniqueId(), (int) Math.round(amount));
            return Boolean.TRUE.equals(res);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "Failed to deposit to PlayerPoints", e);
            return false;
        }
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (!isAvailable() || player == null || takeMethod == null || amount < 0) return false;
        try {
            Object res = takeMethod.invoke(pointsApi, player.getUniqueId(), (int) Math.round(amount));
            return Boolean.TRUE.equals(res);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.FINE, "Failed to withdraw from PlayerPoints", e);
            return false;
        }
    }
}
