package ru.atomicsqd.glowtrade.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ru.atomicsqd.glowtrade.config.ConfigManager;
import ru.atomicsqd.glowtrade.hook.CombatHook;
import ru.atomicsqd.glowtrade.manager.TradeManager;
import ru.atomicsqd.glowtrade.model.TradeSession;
import ru.atomicsqd.glowtrade.util.ItemUtil;

import java.util.Set;

/**
 * Главный слушатель событий плагина GlowTrade.
 * Реализует абсолютную защиту от дюпов, анти-скам и валидацию взаимодействий.
 */
public class TradeListener implements Listener {

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final TradeManager tradeManager;
    private final CombatHook combatHook;

    public TradeListener(Plugin plugin, ConfigManager configManager, TradeManager tradeManager, CombatHook combatHook) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.tradeManager = tradeManager;
        this.combatHook = combatHook;
    }

    /**
     * Быстрая отправка запроса на обмен: Shift + ПКМ по другому игроку.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!configManager.isShiftClickRequest()) {
            return;
        }

        Player sender = event.getPlayer();
        if (!sender.isSneaking()) {
            return;
        }

        if (event.getRightClicked() instanceof Player target) {
            if (!sender.hasPermission("glowtrade.use")) {
                configManager.sendMessage(sender, "no-permission");
                return;
            }

            event.setCancelled(true);
            tradeManager.sendRequest(sender, target);
        }
    }

    /**
     * Защита от дюпов в InventoryClickEvent:
     * - Изоляция слотов предложений
     * - Запрет двойного клика (COLLECT_TO_CURSOR)
     * - Защита от кликов вне инвентаря
     * - Собственная безопасная реализация Shift-клика
     * - Валидация хотбар-свапов (1-9 и 'F')
     * - Проверка черного списка и наковаленных предметов
     * - Сброс готовности при изменении предметов (Anti-Scam)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        TradeSession session = tradeManager.getSession(player);
        if (session == null) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        if (!topInventory.equals(session.getGui().getInventory())) {
            return;
        }

        // 1. Клик вне границ инвентаря
        if (event.getClickedInventory() == null) {
            event.setCancelled(true);
            return;
        }

        // 2. Блокировка потенциально опасных действий
        ClickType clickType = event.getClick();
        InventoryAction action = event.getAction();

        // Запрещаем двойной клик (сбор предметов в курсор со всех слотов)
        if (action == InventoryAction.COLLECT_TO_CURSOR || clickType == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        // Запрещаем свап во вторую руку через клавишу F внутри трейда
        if (clickType == ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
            return;
        }

        // Запрещаем креативные и неизвестные клики
        if (clickType == ClickType.CREATIVE || clickType == ClickType.UNKNOWN) {
            event.setCancelled(true);
            return;
        }

        boolean isTopInventory = event.getClickedInventory().equals(topInventory);
        int slot = event.getSlot();
        Set<Integer> playerOfferSlots = session.getPlayerOfferSlots(player);
        Set<Integer> playerReadySlots = session.getPlayerReadySlots(player);

        // 3. Обработка Shift-клика
        if (event.isShiftClick()) {
            event.setCancelled(true); // Всегда отменяем ванильный Shift-клик!

            if (isTopInventory) {
                // Если игрок нажал Shift-клик на кнопку готовности
                if (playerReadySlots.contains(slot)) {
                    session.toggleReady(player);
                    return;
                }

                // Если игрок нажал Shift-клик на предмет в своем предложении — возвращаем в инвентарь
                if (playerOfferSlots.contains(slot)) {
                    ItemStack clickedItem = topInventory.getItem(slot);
                    if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                        var leftover = player.getInventory().addItem(clickedItem);
                        if (leftover.isEmpty()) {
                            topInventory.setItem(slot, null);
                        } else {
                            clickedItem.setAmount(leftover.get(0).getAmount());
                            topInventory.setItem(slot, clickedItem);
                        }
                        session.onItemsChanged();
                    }
                }
            } else {
                // Игрок нажал Shift-клик по предмету в своем собственном инвентаре (хочет перенести в трейд)
                ItemStack itemToMove = event.getCurrentItem();
                if (itemToMove == null || itemToMove.getType() == Material.AIR) {
                    return;
                }

                // Проверка на черный список
                if (ItemUtil.isBlacklisted(itemToMove, configManager.getBlacklistedMaterials(), configManager.getBlacklistedCmd(), configManager.getBlacklistedTags())) {
                    configManager.sendMessage(player, "item-blacklisted");
                    return;
                }

                // Проверка на переименованные в наковальне предметы
                if (configManager.isBlockAnvilRenamedItems() && ItemUtil.isAnvilRenamed(itemToMove)) {
                    configManager.sendMessage(player, "anvil-item-blocked");
                    return;
                }

                // Безопасный перенос: кладем ТОЛЬКО в разрешенные слоты игрока
                boolean moved = transferItemToOfferSlots(topInventory, playerOfferSlots, itemToMove);
                if (moved) {
                    session.onItemsChanged();
                }
            }
            return;
        }

        // 4. Обработка хотбар-свапа (цифры 1-9)
        if (clickType == ClickType.NUMBER_KEY) {
            if (isTopInventory) {
                if (!playerOfferSlots.contains(slot)) {
                    event.setCancelled(true);
                    return;
                }

                ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
                if (hotbarItem != null && hotbarItem.getType() != Material.AIR) {
                    if (ItemUtil.isBlacklisted(hotbarItem, configManager.getBlacklistedMaterials(), configManager.getBlacklistedCmd(), configManager.getBlacklistedTags())) {
                        event.setCancelled(true);
                        configManager.sendMessage(player, "item-blacklisted");
                        return;
                    }
                    if (configManager.isBlockAnvilRenamedItems() && ItemUtil.isAnvilRenamed(hotbarItem)) {
                        event.setCancelled(true);
                        configManager.sendMessage(player, "anvil-item-blocked");
                        return;
                    }
                }
                session.onItemsChanged();
            }
            return;
        }

        // 5. Обычные клики в верхнем инвентаре
        if (isTopInventory) {
            // Клик по кнопке готовности
            if (playerReadySlots.contains(slot)) {
                event.setCancelled(true);
                session.toggleReady(player);
                return;
            }

            // Клик по чужим кнопкам, разделителю, таймеру или чужим слотам предложения
            if (!playerOfferSlots.contains(slot)) {
                event.setCancelled(true);
                return;
            }

            // Клик по своему слоту предложения
            ItemStack cursorItem = event.getCursor();
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                // Проверка вставляемого предмета
                if (ItemUtil.isBlacklisted(cursorItem, configManager.getBlacklistedMaterials(), configManager.getBlacklistedCmd(), configManager.getBlacklistedTags())) {
                    event.setCancelled(true);
                    configManager.sendMessage(player, "item-blacklisted");
                    return;
                }
                if (configManager.isBlockAnvilRenamedItems() && ItemUtil.isAnvilRenamed(cursorItem)) {
                    event.setCancelled(true);
                    configManager.sendMessage(player, "anvil-item-blocked");
                    return;
                }
            }

            // Сбрасываем готовность при любом изменении в слоте
            Bukkit.getScheduler().runTask(plugin, session::onItemsChanged);
        }
    }

    /**
     * Безопасный перенос предмета в разрешенные слоты предложения.
     */
    private boolean transferItemToOfferSlots(Inventory topInv, Set<Integer> allowedSlots, ItemStack source) {
        int initialAmount = source.getAmount();

        // 1. Попытка стакнуть с уже имеющимися предметами того же типа
        for (int slot : allowedSlots) {
            ItemStack existing = topInv.getItem(slot);
            if (existing != null && existing.isSimilar(source)) {
                int maxStack = existing.getMaxStackSize();
                int space = maxStack - existing.getAmount();
                if (space > 0) {
                    int toAdd = Math.min(space, source.getAmount());
                    existing.setAmount(existing.getAmount() + toAdd);
                    topInv.setItem(slot, existing);
                    source.setAmount(source.getAmount() - toAdd);
                    if (source.getAmount() <= 0) {
                        return true;
                    }
                }
            }
        }

        // 2. Поиск свободных ячеек
        for (int slot : allowedSlots) {
            ItemStack existing = topInv.getItem(slot);
            if (existing == null || existing.getType() == Material.AIR) {
                topInv.setItem(slot, source.clone());
                source.setAmount(0);
                return true;
            }
        }

        return source.getAmount() < initialAmount;
    }

    /**
     * Защита от перетаскивания (InventoryDragEvent):
     * Запрещает задевать любые слоты, кроме собственных слотов предложения.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        TradeSession session = tradeManager.getSession(player);
        if (session == null) {
            return;
        }

        if (!event.getView().getTopInventory().equals(session.getGui().getInventory())) {
            return;
        }

        Set<Integer> allowedSlots = session.getPlayerOfferSlots(player);

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < 54) { // Слот верхнего инвентаря
                if (!allowedSlots.contains(rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Проверка перетаскиваемого предмета
        ItemStack dragged = event.getOldCursor();
        if (dragged != null && dragged.getType() != Material.AIR) {
            if (ItemUtil.isBlacklisted(dragged, configManager.getBlacklistedMaterials(), configManager.getBlacklistedCmd(), configManager.getBlacklistedTags())) {
                event.setCancelled(true);
                configManager.sendMessage(player, "item-blacklisted");
                return;
            }
            if (configManager.isBlockAnvilRenamedItems() && ItemUtil.isAnvilRenamed(dragged)) {
                event.setCancelled(true);
                configManager.sendMessage(player, "anvil-item-blocked");
                return;
            }
        }

        // Сброс таймера при перетаскивании в свои слоты
        Bukkit.getScheduler().runTask(plugin, session::onItemsChanged);
    }

    /**
     * Закрытие инвентаря: мгновенная и безопасная отмена трейда с возвратом всех предметов.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        TradeSession session = tradeManager.getSession(player);
        if (session != null && session.getGui().getInventory().equals(event.getInventory())) {
            // Если сессия еще не завершается или не отменена
            if (session.getState() != TradeSession.State.COMPLETING && session.getState() != TradeSession.State.CANCELLED) {
                session.cancelTrade(configManager.getMessage("trade-cancelled"));
            }
        }
    }

    /**
     * Выход игрока с сервера.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        handleDisconnect(event.getPlayer());
    }

    /**
     * Кик игрока с сервера.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerKick(PlayerKickEvent event) {
        handleDisconnect(event.getPlayer());
    }

    private void handleDisconnect(Player player) {
        TradeSession session = tradeManager.getSession(player);
        if (session != null) {
            session.cancelTrade(configManager.getMessage("player-offline", "%player%", player.getName()));
        }
    }

    /**
     * Смерть игрока.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        TradeSession session = tradeManager.getSession(player);
        if (session != null) {
            session.cancelTrade(configManager.getMessage("trade-cancelled"));
        }
    }

    /**
     * Получение урона во время обмена.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (configManager.isCancelOnDamage()) {
                TradeSession session = tradeManager.getSession(player);
                if (session != null) {
                    session.cancelTrade(configManager.getMessage("trade-cancelled"));
                }
            }
        }
    }

    /**
     * Трекинг боя в PvP.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvPDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim) {
            combatHook.tagPlayer(victim);
            if (event.getDamager() instanceof Player attacker) {
                combatHook.tagPlayer(attacker);
            }
        }
    }

    /**
     * Проверка дистанции между игроками при перемещении.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!configManager.isCancelOnDistanceExceeded()) {
            return;
        }

        // Проверяем только существенное смещение блока
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        TradeSession session = tradeManager.getSession(player);
        if (session != null) {
            Player opponent = session.getOpponent(player);
            if (opponent == null || !opponent.isOnline() || !player.getWorld().equals(opponent.getWorld()) ||
                player.getLocation().distanceSquared(opponent.getLocation()) > Math.pow(configManager.getMaxDistance(), 2)) {
                session.cancelTrade(configManager.getMessage("too-far", "%distance%", String.valueOf((int) configManager.getMaxDistance())));
            }
        }
    }
}
