package ru.atomicsqd.glowtrade.manager;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import ru.atomicsqd.glowtrade.config.ConfigManager;
import ru.atomicsqd.glowtrade.hook.CombatHook;
import ru.atomicsqd.glowtrade.model.TradeRequest;
import ru.atomicsqd.glowtrade.model.TradeSession;
import ru.atomicsqd.glowtrade.util.SoundUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер запросов и активных сессий обмена.
 */
public class TradeManager {

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final CombatHook combatHook;

    // Входящие запросы: Target UUID -> (Sender UUID -> TradeRequest)
    private final Map<UUID, Map<UUID, TradeRequest>> incomingRequests = new ConcurrentHashMap<>();

    // Активные сессии обмена: Player UUID -> TradeSession
    private final Map<UUID, TradeSession> activeSessions = new ConcurrentHashMap<>();

    // Игроки, отключившие трейд (/trade toggle)
    private final Set<UUID> disabledTrades = ConcurrentHashMap.newKeySet();

    public TradeManager(Plugin plugin, ConfigManager configManager, CombatHook combatHook) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.combatHook = combatHook;
    }

    /**
     * Проверяет, участвует ли игрок в активном обмене.
     */
    public boolean isInTrade(Player player) {
        if (player == null) return false;
        return activeSessions.containsKey(player.getUniqueId());
    }

    /**
     * Получить активную сессию игрока.
     */
    public TradeSession getSession(Player player) {
        if (player == null) return null;
        return activeSessions.get(player.getUniqueId());
    }

    /**
     * Удалить завершенную или отмененную сессию.
     */
    public void removeSession(TradeSession session) {
        if (session != null) {
            activeSessions.remove(session.getPlayer1().getUniqueId());
            activeSessions.remove(session.getPlayer2().getUniqueId());
        }
    }

    /**
     * Переключить прием запросов на обмен для игрока.
     */
    public boolean toggleTrades(Player player) {
        UUID uuid = player.getUniqueId();
        if (disabledTrades.contains(uuid)) {
            disabledTrades.remove(uuid);
            configManager.sendMessage(player, "toggle-on");
            return true;
        } else {
            disabledTrades.add(uuid);
            configManager.sendMessage(player, "toggle-off");
            return false;
        }
    }

    public boolean hasDisabledTrades(Player player) {
        return disabledTrades.contains(player.getUniqueId());
    }

    /**
     * Отправка запроса на обмен.
     */
    public void sendRequest(Player sender, Player target) {
        // 1. Проверка на самого себя
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            configManager.sendMessage(sender, "cannot-trade-self");
            return;
        }

        // 2. Проверка онлайна
        if (!target.isOnline()) {
            configManager.sendMessage(sender, "player-offline", "%player%", target.getName());
            return;
        }

        // 3. Проверка /trade toggle
        if (disabledTrades.contains(sender.getUniqueId())) {
            configManager.sendMessage(sender, "you-disabled-trades");
            return;
        }
        if (disabledTrades.contains(target.getUniqueId())) {
            configManager.sendMessage(sender, "player-disabled-trades", "%player%", target.getName());
            return;
        }

        // 4. Проверка на уже идущий трейд
        if (isInTrade(sender) || isInTrade(target)) {
            configManager.sendMessage(sender, "already-in-trade");
            return;
        }

        // 5. Проверка режима игры
        if (configManager.isPreventCreative() && (sender.getGameMode() == GameMode.CREATIVE || target.getGameMode() == GameMode.CREATIVE)) {
            configManager.sendMessage(sender, "creative-forbidden");
            return;
        }
        if (configManager.isPreventSpectator() && (sender.getGameMode() == GameMode.SPECTATOR || target.getGameMode() == GameMode.SPECTATOR)) {
            configManager.sendMessage(sender, "spectator-forbidden");
            return;
        }

        // 6. Проверка режима боя
        if (configManager.isPreventInCombat()) {
            if (combatHook.isInCombat(sender)) {
                configManager.sendMessage(sender, "combat-forbidden");
                return;
            }
            if (combatHook.isInCombat(target)) {
                configManager.sendMessage(sender, "target-combat-forbidden", "%player%", target.getName());
                return;
            }
        }

        // 7. Проверка расстояния и мира
        if (configManager.isDistanceEnabled()) {
            if (configManager.isRequireSameWorld() && !sender.getWorld().equals(target.getWorld())) {
                configManager.sendMessage(sender, "different-world");
                return;
            }
            if (sender.getWorld().equals(target.getWorld())) {
                double distSq = sender.getLocation().distanceSquared(target.getLocation());
                if (distSq > Math.pow(configManager.getMaxDistance(), 2)) {
                    configManager.sendMessage(sender, "too-far", "%distance%", String.valueOf((int) configManager.getMaxDistance()));
                    return;
                }
            }
        }

        // Взаимный запрос: если цель уже отправляла запрос отправителю — мгновенно начинаем трейд!
        Map<UUID, TradeRequest> senderIncoming = incomingRequests.get(sender.getUniqueId());
        if (senderIncoming != null && senderIncoming.containsKey(target.getUniqueId())) {
            TradeRequest existing = senderIncoming.remove(target.getUniqueId());
            existing.cancelExpiryTask();
            startTrade(sender, target);
            return;
        }

        // Проверка повторного запроса
        Map<UUID, TradeRequest> targetRequests = incomingRequests.computeIfAbsent(target.getUniqueId(), k -> new ConcurrentHashMap<>());
        if (targetRequests.containsKey(sender.getUniqueId())) {
            configManager.sendMessage(sender, "request-already-sent");
            return;
        }

        // Создаем запрос
        TradeRequest request = new TradeRequest(sender.getUniqueId(), target.getUniqueId());
        int timeout = configManager.getRequestTimeout();

        BukkitTask expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Map<UUID, TradeRequest> reqs = incomingRequests.get(target.getUniqueId());
            if (reqs != null && reqs.remove(sender.getUniqueId()) != null) {
                if (sender.isOnline()) {
                    configManager.sendMessage(sender, "request-expired", "%player%", target.getName());
                }
            }
        }, timeout * 20L);

        request.setExpiryTask(expiryTask);
        targetRequests.put(sender.getUniqueId(), request);

        // Звуки и сообщения
        if (configManager.isSoundsEnabled()) {
            SoundUtil.playSound(sender, configManager.getSoundName("request-sent"), 1.0f, 1.2f);
            SoundUtil.playSound(target, configManager.getSoundName("request-received"), 1.0f, 1.0f);
        }

        configManager.sendMessage(sender, "request-sent", "%player%", target.getName());
        configManager.sendMessage(target, "request-received", "%player%", sender.getName());
    }

    /**
     * Принять запрос на обмен.
     */
    public void acceptRequest(Player target, String senderName) {
        Map<UUID, TradeRequest> reqs = incomingRequests.get(target.getUniqueId());
        if (reqs == null || reqs.isEmpty()) {
            configManager.sendMessage(target, "no-pending-request");
            return;
        }

        TradeRequest targetReq = null;
        if (senderName != null) {
            Player sender = Bukkit.getPlayer(senderName);
            if (sender == null || !reqs.containsKey(sender.getUniqueId())) {
                configManager.sendMessage(target, "no-pending-request");
                return;
            }
            targetReq = reqs.remove(sender.getUniqueId());
        } else {
            // Берем самый первый актуальный запрос
            Iterator<TradeRequest> it = reqs.values().iterator();
            if (it.hasNext()) {
                targetReq = it.next();
                it.remove();
            }
        }

        if (targetReq == null) {
            configManager.sendMessage(target, "no-pending-request");
            return;
        }

        targetReq.cancelExpiryTask();
        Player sender = Bukkit.getPlayer(targetReq.getSender());

        if (sender == null || !sender.isOnline()) {
            configManager.sendMessage(target, "player-offline", "%player%", "игрок");
            return;
        }

        // Проверяем валидность перед стартом
        if (isInTrade(sender) || isInTrade(target)) {
            configManager.sendMessage(target, "already-in-trade");
            return;
        }

        startTrade(sender, target);
    }

    /**
     * Отклонить запрос на обмен.
     */
    public void denyRequest(Player target, String senderName) {
        Map<UUID, TradeRequest> reqs = incomingRequests.get(target.getUniqueId());
        if (reqs == null || reqs.isEmpty()) {
            configManager.sendMessage(target, "no-pending-request");
            return;
        }

        TradeRequest targetReq = null;
        if (senderName != null) {
            Player sender = Bukkit.getPlayer(senderName);
            if (sender == null || !reqs.containsKey(sender.getUniqueId())) {
                configManager.sendMessage(target, "no-pending-request");
                return;
            }
            targetReq = reqs.remove(sender.getUniqueId());
        } else {
            Iterator<TradeRequest> it = reqs.values().iterator();
            if (it.hasNext()) {
                targetReq = it.next();
                it.remove();
            }
        }

        if (targetReq == null) {
            configManager.sendMessage(target, "no-pending-request");
            return;
        }

        targetReq.cancelExpiryTask();
        Player sender = Bukkit.getPlayer(targetReq.getSender());

        configManager.sendMessage(target, "request-denied-target", "%player%", sender != null ? sender.getName() : "игрок");
        if (sender != null && sender.isOnline()) {
            configManager.sendMessage(sender, "request-denied", "%player%", target.getName());
        }
    }

    /**
     * Инициализирует и начинает сессию обмена.
     */
    private void startTrade(Player player1, Player player2) {
        // Очищаем все висящие запросы между этими игроками
        clearRequestsFor(player1.getUniqueId());
        clearRequestsFor(player2.getUniqueId());

        TradeSession session = new TradeSession(plugin, configManager, this, player1, player2);
        activeSessions.put(player1.getUniqueId(), session);
        activeSessions.put(player2.getUniqueId(), session);

        session.start();
    }

    private void clearRequestsFor(UUID uuid) {
        Map<UUID, TradeRequest> reqs = incomingRequests.remove(uuid);
        if (reqs != null) {
            reqs.values().forEach(TradeRequest::cancelExpiryTask);
        }
    }

    /**
     * Отменяет все активные обмены (при выключении плагина или сервера).
     */
    public void cancelAllTrades(String reason) {
        Set<TradeSession> sessions = new HashSet<>(activeSessions.values());
        for (TradeSession session : sessions) {
            session.cancelTrade(reason);
        }
        activeSessions.clear();
        incomingRequests.clear();
    }
}
