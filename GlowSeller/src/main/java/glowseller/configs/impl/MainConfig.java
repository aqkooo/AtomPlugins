package glowseller.configs.impl;

import glowseller.Main;
import glowseller.configs.CustomConfig;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class MainConfig extends CustomConfig {
    public enum StackingMode {
        EXTEND,
        REPLACE,
        DENY
    }

    private String economyType;
    private StackingMode boosterStacking;
    private final Map<String, Double> permissionBoosters = new HashMap<>();
    private int saveIntervalSeconds;
    private String locale;

    private String sellSound;
    private String buySound;
    private String errorSound;
    private String expireSound;
    private boolean actionbarOnBoosterExpire;

    public MainConfig(Main plugin) {
        super(plugin, "config.yml");
        reload();
    }

    @Override
    public void parse() {
        economyType = config.getString("economy.type", "VAULT").toUpperCase();
        
        String stackingStr = config.getString("boosters.stacking", "EXTEND").toUpperCase();
        try {
            boosterStacking = StackingMode.valueOf(stackingStr);
        } catch (IllegalArgumentException e) {
            boosterStacking = StackingMode.EXTEND;
        }

        permissionBoosters.clear();
        ConfigurationSection permSec = config.getConfigurationSection("boosters.permission_boosters");
        if (permSec != null) {
            for (String key : permSec.getKeys(false)) {
                String perm = permSec.getString(key + ".permission", "glowseller.booster." + key);
                double mult = permSec.getDouble(key + ".multiplier", 1.0);
                permissionBoosters.put(perm, mult);
            }
        }

        saveIntervalSeconds = config.getInt("settings.save_interval_seconds", 120);
        locale = config.getString("locale", "ru");

        sellSound = config.getString("sounds.sell", "ENTITY_EXPERIENCE_ORB_PICKUP");
        buySound = config.getString("sounds.buy", "ENTITY_PLAYER_LEVELUP");
        errorSound = config.getString("sounds.error", "ENTITY_VILLAGER_NO");
        expireSound = config.getString("sounds.booster_expire", "BLOCK_ANVIL_LAND");
        actionbarOnBoosterExpire = config.getBoolean("messages.actionbar_on_booster_expire", true);
    }

    public String getEconomyType() {
        return economyType;
    }

    public StackingMode getBoosterStacking() {
        return boosterStacking;
    }

    public Map<String, Double> getPermissionBoosters() {
        return permissionBoosters;
    }

    public int getSaveIntervalSeconds() {
        return saveIntervalSeconds;
    }

    public String getLocale() {
        return locale;
    }

    public String getSellSound() {
        return sellSound;
    }

    public String getBuySound() {
        return buySound;
    }

    public String getErrorSound() {
        return errorSound;
    }

    public String getExpireSound() {
        return expireSound;
    }

    public boolean isActionbarOnBoosterExpire() {
        return actionbarOnBoosterExpire;
    }
}
