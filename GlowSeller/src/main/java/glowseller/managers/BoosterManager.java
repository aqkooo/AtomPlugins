package glowseller.managers;

import glowseller.Main;
import glowseller.configs.impl.MainConfig;
import glowseller.models.PlayerData;
import glowseller.models.ShopItem;
import org.bukkit.entity.Player;

import java.util.Map;

public class BoosterManager {
    private final Main plugin;

    public BoosterManager(Main plugin) {
        this.plugin = plugin;
    }

    public double getPermissionMultiplier(Player player) {
        if (player == null) return 1.0;
        double maxMult = 1.0;

        Map<String, Double> permBoosters = plugin.getMainConfig().getPermissionBoosters();
        for (Map.Entry<String, Double> entry : permBoosters.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                if (entry.getValue() > maxMult) {
                    maxMult = entry.getValue();
                }
            }
        }

        return maxMult;
    }

    public double getActiveBoosterMultiplier(Player player) {
        if (player == null) return 1.0;
        PlayerData data = plugin.getPlayerDataCache().get(player.getUniqueId());
        if (data == null || !data.hasActiveBooster()) return 1.0;

        ShopItem item = plugin.getShopConfig().getItem(data.getActiveBoosterKey());
        if (item != null && item.getType() == ShopItem.Type.BOOSTER) {
            return Math.max(1.0, item.getMultiplier());
        }

        return 1.0;
    }

    public double getTotalMultiplier(Player player) {
        double perm = getPermissionMultiplier(player);
        double purchased = getActiveBoosterMultiplier(player);

        double total = perm + purchased - 1.0;
        return Math.max(1.0, total);
    }

    public boolean canApplyBooster(Player player, String boosterKey) {
        if (player == null) return false;
        PlayerData data = plugin.getPlayerDataCache().get(player.getUniqueId());
        if (data == null) return false;

        if (!data.hasActiveBooster()) {
            return true;
        }

        MainConfig.StackingMode mode = plugin.getMainConfig().getBoosterStacking();
        return mode != MainConfig.StackingMode.DENY;
    }

    public boolean applyBooster(Player player, String boosterKey, double multiplier, long durationSeconds) {
        if (player == null) return false;
        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());
        MainConfig.StackingMode mode = plugin.getMainConfig().getBoosterStacking();

        long durationMillis = durationSeconds * 1000L;
        long now = System.currentTimeMillis();

        if (data.hasActiveBooster()) {
            switch (mode) {
                case DENY -> {
                    return false;
                }
                case EXTEND -> {
                    long currentExpire = data.getBoosterExpireAt();
                    long newExpire = Math.max(now, currentExpire) + durationMillis;
                    data.setBoosterExpireAt(newExpire);
                    // Keep the higher booster or update to new
                    ShopItem oldItem = plugin.getShopConfig().getItem(data.getActiveBoosterKey());
                    double oldMult = oldItem != null ? oldItem.getMultiplier() : 1.0;
                    if (multiplier >= oldMult) {
                        data.setActiveBoosterKey(boosterKey);
                    }
                }
                case REPLACE -> {
                    data.setActiveBoosterKey(boosterKey);
                    data.setBoosterExpireAt(now + durationMillis);
                }
            }
        } else {
            data.setActiveBoosterKey(boosterKey);
            data.setBoosterExpireAt(now + durationMillis);
        }

        return true;
    }

    public String formatTime(long totalSeconds) {
        if (totalSeconds <= 0) return "0с";

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("ч ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("м ");
        }
        if (seconds > 0 || (hours == 0 && minutes == 0)) {
            sb.append(seconds).append("с");
        }

        return sb.toString().trim();
    }
}
