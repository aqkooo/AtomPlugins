package ru.atomicsqd.glowtrade.model;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import ru.atomicsqd.glowtrade.config.ConfigManager;
import ru.atomicsqd.glowtrade.gui.TradeGUI;
import ru.atomicsqd.glowtrade.manager.TradeManager;
import ru.atomicsqd.glowtrade.util.ItemUtil;
import ru.atomicsqd.glowtrade.util.SoundUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Сессия безопасного обмена между двумя игроками.
 * Обеспечивает полную защиту от дюпов, атомарность передачи ресурсов и анти-скам.
 */
public class TradeSession {

    public enum State {
        PREPARING,
        COUNTDOWN,
        COMPLETING,
        CANCELLED
    }

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final TradeManager tradeManager;

    private final Player player1;
    private final Player player2;
    private final TradeGUI gui;

    private volatile boolean ready1 = false;
    private volatile boolean ready2 = false;

    private final AtomicReference<State> state = new AtomicReference<>(State.PREPARING);

    private BukkitTask countdownTask;
    private int secondsRemaining;

    public TradeSession(Plugin plugin, ConfigManager configManager, TradeManager tradeManager, Player player1, Player player2) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.tradeManager = tradeManager;
        this.player1 = player1;
        this.player2 = player2;
        this.gui = new TradeGUI(configManager, player1, player2);
    }

    /**
     * Запуск сессии обмена и открытие меню обоим игрокам.
     */
    public void start() {
        player1.openInventory(gui.getInventory());
        player2.openInventory(gui.getInventory());
    }

    public boolean isParticipant(Player player) {
        if (player == null) return false;
        return player.getUniqueId().equals(player1.getUniqueId()) ||
               player.getUniqueId().equals(player2.getUniqueId());
    }

    public boolean isPlayer1(Player player) {
        return player != null && player.getUniqueId().equals(player1.getUniqueId());
    }

    public Player getOpponent(Player player) {
        return isPlayer1(player) ? player2 : player1;
    }

    public Set<Integer> getPlayerOfferSlots(Player player) {
        return isPlayer1(player) ? TradeGUI.PLAYER_1_OFFER_SLOTS : TradeGUI.PLAYER_2_OFFER_SLOTS;
    }

    public Set<Integer> getPlayerReadySlots(Player player) {
        return isPlayer1(player) ? TradeGUI.PLAYER_1_READY_SLOTS : TradeGUI.PLAYER_2_READY_SLOTS;
    }

    public boolean isReady(Player player) {
        return isPlayer1(player) ? ready1 : ready2;
    }

    public State getState() {
        return state.get();
    }

    public TradeGUI getGui() {
        return gui;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    /**
     * Переключение статуса готовности игрока.
     */
    public synchronized void toggleReady(Player player) {
        if (state.get() == State.COMPLETING || state.get() == State.CANCELLED) {
            return;
        }

        if (isPlayer1(player)) {
            ready1 = !ready1;
        } else {
            ready2 = !ready2;
        }

        // Звук переключения готовности
        if (configManager.isSoundsEnabled()) {
            SoundUtil.playSound(player1, configManager.getSoundName("ready-toggle"), 1.0f, 1.2f);
            SoundUtil.playSound(player2, configManager.getSoundName("ready-toggle"), 1.0f, 1.2f);
        }

        gui.updateReadyButtons(ready1, ready2);

        // Если оба игрока готовы — запускаем обратный отсчет
        if (ready1 && ready2) {
            startCountdown();
        } else {
            // Если кто-то снял готовность во время отсчета
            if (state.get() == State.COUNTDOWN) {
                cancelCountdown();
            }
        }
    }

    /**
     * Anti-Scam логика: сброс готовности и таймера при любом изменении предметов.
     */
    public synchronized void onItemsChanged() {
        if (state.get() == State.COMPLETING || state.get() == State.CANCELLED) {
            return;
        }

        if (ready1 || ready2 || state.get() == State.COUNTDOWN) {
            ready1 = false;
            ready2 = false;
            cancelCountdown();
            gui.updateReadyButtons(false, false);
            gui.updateTimerIndicator(false, 0);

            // Оповещение об изменении
            if (configManager.isSoundsEnabled()) {
                SoundUtil.playSound(player1, configManager.getSoundName("anti-scam-alert"), 1.0f, 0.8f);
                SoundUtil.playSound(player2, configManager.getSoundName("anti-scam-alert"), 1.0f, 0.8f);
            }

            configManager.sendMessage(player1, "anti-scam-triggered");
            configManager.sendMessage(player2, "anti-scam-triggered");
        }
    }

    /**
     * Запуск 3-секундного обратного отсчета перед совершением обмена.
     */
    private synchronized void startCountdown() {
        if (!state.compareAndSet(State.PREPARING, State.COUNTDOWN)) {
            return;
        }

        secondsRemaining = configManager.getCountdownSeconds();
        gui.updateTimerIndicator(true, secondsRemaining);

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Проверка валидности игроков на каждом тике
            if (!validatePlayers()) {
                cancelTrade(configManager.getMessage("trade-cancelled"));
                return;
            }

            if (secondsRemaining <= 1) {
                // Таймер завершился — производим обмен!
                completeTrade();
            } else {
                secondsRemaining--;
                gui.updateTimerIndicator(true, secondsRemaining);

                if (configManager.isSoundsEnabled()) {
                    SoundUtil.playSound(player1, configManager.getSoundName("timer-tick"), 1.0f, 1.5f);
                    SoundUtil.playSound(player2, configManager.getSoundName("timer-tick"), 1.0f, 1.5f);
                }
            }
        }, 20L, 20L);
    }

    /**
     * Остановка обратного отсчета.
     */
    private synchronized void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        state.set(State.PREPARING);
        gui.updateTimerIndicator(false, 0);
    }

    /**
     * Проверяет доступность игроков для продолжения обмена.
     */
    private boolean validatePlayers() {
        if (!player1.isOnline() || !player2.isOnline()) return false;
        if (player1.isDead() || player2.isDead()) return false;

        // Проверка дистанции
        if (configManager.isDistanceEnabled()) {
            if (configManager.isRequireSameWorld() && !player1.getWorld().equals(player2.getWorld())) {
                return false;
            }
            if (player1.getWorld().equals(player2.getWorld())) {
                if (player1.getLocation().distanceSquared(player2.getLocation()) > Math.pow(configManager.getMaxDistance(), 2)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Атомарное завершение обмена.
     */
    public synchronized void completeTrade() {
        if (!state.compareAndSet(State.COUNTDOWN, State.COMPLETING)) {
            return;
        }

        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        // Атомарно извлекаем предметы из слотов предложений
        List<ItemStack> p1Items = new ArrayList<>();
        for (int slot : TradeGUI.PLAYER_1_OFFER_SLOTS) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                p1Items.add(item.clone());
                gui.getInventory().setItem(slot, null); // Очищаем слот во избежание дюпа
            }
        }

        List<ItemStack> p2Items = new ArrayList<>();
        for (int slot : TradeGUI.PLAYER_2_OFFER_SLOTS) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                p2Items.add(item.clone());
                gui.getInventory().setItem(slot, null); // Очищаем слот во избежание дюпа
            }
        }

        // Передаем предметы P1 игроку P2
        for (ItemStack item : p1Items) {
            ItemUtil.giveOrDropItem(player2, item);
        }

        // Передаем предметы P2 игроку P1
        for (ItemStack item : p2Items) {
            ItemUtil.giveOrDropItem(player1, item);
        }

        // Удаляем сессию из активных
        tradeManager.removeSession(this);

        // Закрываем инвентари в следующем тике во избежание рекурсии
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player1.isOnline()) player1.closeInventory();
            if (player2.isOnline()) player2.closeInventory();
        });

        // Воспроизводим победный звук и отправляем сообщения
        if (configManager.isSoundsEnabled()) {
            SoundUtil.playSound(player1, configManager.getSoundName("trade-success"), 1.0f, 1.0f);
            SoundUtil.playSound(player2, configManager.getSoundName("trade-success"), 1.0f, 1.0f);
        }

        configManager.sendMessage(player1, "trade-success", "%player%", player2.getName());
        configManager.sendMessage(player2, "trade-success", "%player%", player1.getName());
    }

    /**
     * Атомарная отмена обмена с гарантированным возвратом всех выставленных предметов.
     */
    public synchronized void cancelTrade(String reason) {
        State previous = state.getAndSet(State.CANCELLED);
        if (previous == State.CANCELLED || previous == State.COMPLETING) {
            return;
        }

        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        // Извлекаем и возвращаем предметы игрока 1
        for (int slot : TradeGUI.PLAYER_1_OFFER_SLOTS) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                gui.getInventory().setItem(slot, null); // Защита от повторного сбора
                if (player1.isOnline()) {
                    ItemUtil.giveOrDropItem(player1, item);
                } else {
                    // Если игрок вылетел — дропаем в его последней локации
                    player1.getWorld().dropItemNaturally(player1.getLocation(), item);
                }
            }
        }

        // Извлекаем и возвращаем предметы игрока 2
        for (int slot : TradeGUI.PLAYER_2_OFFER_SLOTS) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                gui.getInventory().setItem(slot, null); // Защита от повторного сбора
                if (player2.isOnline()) {
                    ItemUtil.giveOrDropItem(player2, item);
                } else {
                    player2.getWorld().dropItemNaturally(player2.getLocation(), item);
                }
            }
        }

        tradeManager.removeSession(this);

        // Безопасное закрытие инвентарей
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player1.isOnline() && player1.getOpenInventory().getTopInventory().equals(gui.getInventory())) {
                player1.closeInventory();
            }
            if (player2.isOnline() && player2.getOpenInventory().getTopInventory().equals(gui.getInventory())) {
                player2.closeInventory();
            }
        });

        if (configManager.isSoundsEnabled()) {
            SoundUtil.playSound(player1, configManager.getSoundName("trade-cancel"), 1.0f, 0.8f);
            SoundUtil.playSound(player2, configManager.getSoundName("trade-cancel"), 1.0f, 0.8f);
        }

        if (reason != null && !reason.isEmpty()) {
            if (player1.isOnline()) player1.sendMessage(reason);
            if (player2.isOnline()) player2.sendMessage(reason);
        } else {
            configManager.sendMessage(player1, "trade-cancelled");
            configManager.sendMessage(player2, "trade-cancelled");
        }
    }
}
