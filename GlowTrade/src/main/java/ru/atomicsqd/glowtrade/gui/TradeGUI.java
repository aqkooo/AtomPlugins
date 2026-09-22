package ru.atomicsqd.glowtrade.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.atomicsqd.glowtrade.config.ConfigManager;
import ru.atomicsqd.glowtrade.util.ItemUtil;

import java.util.*;

/**
 * Класс отрисовки и управления графическим интерфейсом обмена (54 слота, двойной сундук).
 * Оформлен в фирменном стиле GlowTrade с градиентами и защитой от наковаленного курсива.
 */
public class TradeGUI {

    // Слоты предложений игроков (по 16 слотов каждому)
    public static final Set<Integer> PLAYER_1_OFFER_SLOTS = Set.of(
            0, 1, 2, 3,
            9, 10, 11, 12,
            18, 19, 20, 21,
            27, 28, 29, 30
    );

    public static final Set<Integer> PLAYER_2_OFFER_SLOTS = Set.of(
            5, 6, 7, 8,
            14, 15, 16, 17,
            23, 24, 25, 26,
            32, 33, 34, 35
    );

    // Разделительная полоса по центру
    public static final Set<Integer> SEPARATOR_SLOTS = Set.of(
            4, 13, 22, 31, 49
    );

    // Кнопки готовности
    public static final Set<Integer> PLAYER_1_READY_SLOTS = Set.of(
            36, 37, 38, 39
    );

    public static final Set<Integer> PLAYER_2_READY_SLOTS = Set.of(
            41, 42, 43, 44
    );

    // Слот таймера подтверждения
    public static final int TIMER_SLOT = 40;

    // Нижние декоративные слоты заполнители
    public static final Set<Integer> FILLER_SLOTS = Set.of(
            45, 46, 47, 48,
            50, 51, 52, 53
    );

    private final ConfigManager configManager;
    private final Inventory inventory;
    private final Player player1;
    private final Player player2;

    public TradeGUI(ConfigManager configManager, Player player1, Player player2) {
        this.configManager = configManager;
        this.player1 = player1;
        this.player2 = player2;
        this.inventory = Bukkit.createInventory(null, 54, configManager.getGuiTitle());

        setupDecorations();
        updateReadyButtons(false, false);
        updateTimerIndicator(false, 0);
    }

    /**
     * Заполняет разделительные полосы и декоративные слоты.
     */
    private void setupDecorations() {
        ItemStack separator = ItemUtil.createGuiItem(
                configManager.getSeparatorMaterial(),
                configManager.getMessage("gui.separator-name"),
                Collections.emptyList()
        );

        for (int slot : SEPARATOR_SLOTS) {
            inventory.setItem(slot, separator);
        }

        ItemStack filler = ItemUtil.createGuiItem(
                configManager.getFillerMaterial(),
                configManager.getMessage("gui.filler-name"),
                Collections.emptyList()
        );

        for (int slot : FILLER_SLOTS) {
            inventory.setItem(slot, filler);
        }
    }

    /**
     * Обновляет статус кнопок готовности для обоих игроков.
     */
    public void updateReadyButtons(boolean ready1, boolean ready2) {
        // Кнопки игрока 1
        Material mat1 = ready1 ? configManager.getReadyMaterial() : configManager.getNotReadyMaterial();
        String title1 = configManager.getMessage(
                ready1 ? "gui.status-ready-name" : "gui.status-not-ready-name",
                "%player%", player1.getName()
        );
        List<String> lore1 = configManager.getMessageList(
                ready1 ? "gui.status-ready-lore" : "gui.status-not-ready-lore",
                "%player%", player1.getName()
        );

        ItemStack btn1 = ItemUtil.createGuiItem(mat1, 1, title1, lore1);
        for (int slot : PLAYER_1_READY_SLOTS) {
            inventory.setItem(slot, btn1);
        }

        // Кнопки игрока 2
        Material mat2 = ready2 ? configManager.getReadyMaterial() : configManager.getNotReadyMaterial();
        String title2 = configManager.getMessage(
                ready2 ? "gui.status-ready-name" : "gui.status-not-ready-name",
                "%player%", player2.getName()
        );
        List<String> lore2 = configManager.getMessageList(
                ready2 ? "gui.status-ready-lore" : "gui.status-not-ready-lore",
                "%player%", player2.getName()
        );

        ItemStack btn2 = ItemUtil.createGuiItem(mat2, 1, title2, lore2);
        for (int slot : PLAYER_2_READY_SLOTS) {
            inventory.setItem(slot, btn2);
        }
    }

    /**
     * Обновляет индикатор таймера по центру (слот 40).
     */
    public void updateTimerIndicator(boolean countingDown, int secondsLeft) {
        if (!countingDown) {
            ItemStack waitingItem = ItemUtil.createGuiItem(
                    configManager.getTimerWaitingMaterial(),
                    1,
                    configManager.getMessage("gui.timer-waiting-name"),
                    configManager.getMessageList("gui.timer-waiting-lore")
            );
            inventory.setItem(TIMER_SLOT, waitingItem);
        } else {
            ItemStack countdownItem = ItemUtil.createGuiItem(
                    configManager.getTimerActiveMaterial(),
                    Math.max(1, secondsLeft),
                    configManager.getMessage("gui.timer-countdown-name", "%seconds%", String.valueOf(secondsLeft)),
                    configManager.getMessageList("gui.timer-countdown-lore", "%seconds%", String.valueOf(secondsLeft))
            );
            inventory.setItem(TIMER_SLOT, countdownItem);
        }
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }
}
