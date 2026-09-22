package ru.atomicsqd.glowtrade.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Интеграция с PvP-плагинами (CombatLogX, PvPManager) и встроенный трекер боя.
 */
public class CombatHook {

    private final boolean combatLogXEnabled;
    private final boolean pvpManagerEnabled;

    // Встроенный трекер последнего времени боя
    private final Map<UUID, Long> combatTagMap = new ConcurrentHashMap<>();
    private long internalCombatDurationMillis = 10000; // 10 секунд по умолчанию

    public CombatHook() {
        this.combatLogXEnabled = Bukkit.getPluginManager().isPluginEnabled("CombatLogX");
        this.pvpManagerEnabled = Bukkit.getPluginManager().isPluginEnabled("PvPManager");
    }

    /**
     * Помечает игрока как находящегося в бою (для внутреннего трекера).
     */
    public void tagPlayer(Player player) {
        if (player != null) {
            combatTagMap.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    /**
     * Очищает метку боя.
     */
    public void untagPlayer(Player player) {
        if (player != null) {
            combatTagMap.remove(player.getUniqueId());
        }
    }

    /**
     * Проверяет, находится ли игрок в состоянии PvP боя.
     */
    public boolean isInCombat(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        // 1. Проверка через CombatLogX
        if (combatLogXEnabled) {
            try {
                Plugin clx = Bukkit.getPluginManager().getPlugin("CombatLogX");
                if (clx != null) {
                    Method getCombatManager = clx.getClass().getMethod("getCombatManager");
                    Object combatManager = getCombatManager.invoke(clx);
                    Method isInCombat = combatManager.getClass().getMethod("isInCombat", Player.class);
                    Object result = isInCombat.invoke(combatManager, player);
                    if (result instanceof Boolean && (Boolean) result) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. Проверка через PvPManager
        if (pvpManagerEnabled) {
            try {
                Plugin pvpPlugin = Bukkit.getPluginManager().getPlugin("PvPManager");
                if (pvpPlugin != null) {
                    Method getInstance = pvpPlugin.getClass().getMethod("getInstance");
                    Object instance = getInstance.invoke(null);
                    Method getPlayerHandler = instance.getClass().getMethod("getPlayerHandler");
                    Object playerHandler = getPlayerHandler.invoke(instance);
                    Method getPvPlayer = playerHandler.getClass().getMethod("get", Player.class);
                    Object pvPlayer = getPvPlayer.invoke(playerHandler, player);
                    if (pvPlayer != null) {
                        Method isInCombat = pvPlayer.getClass().getMethod("isInCombat");
                        Object result = isInCombat.invoke(pvPlayer);
                        if (result instanceof Boolean && (Boolean) result) {
                            return true;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. Внутренний трекер урона
        Long tagTime = combatTagMap.get(player.getUniqueId());
        if (tagTime != null) {
            if (System.currentTimeMillis() - tagTime < internalCombatDurationMillis) {
                return true;
            } else {
                combatTagMap.remove(player.getUniqueId());
            }
        }

        return false;
    }
}
