package glowseller.menu;

import glowseller.Main;
import glowseller.events.PointsUpdateEvent;
import glowseller.events.SellEvent;
import glowseller.models.PlayerData;
import glowseller.models.item.Item;
import glowseller.utils.Colorizer;
import glowseller.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class SellMenu extends AbstractMenu {
    private boolean isNavigatingAway = false;

    public SellMenu(Main plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public String getTitle() {
        String customTitle = plugin.getMessageConfig().getRaw("gui.sell_title");
        if (customTitle != null && !customTitle.isEmpty()) {
            return Colorizer.colorize(customTitle);
        }
        return Colorizer.colorize("Положи предметы для продажи");
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public boolean isInteractive() {
        return true;
    }

    @Override
    public boolean isSlotInteractive(int rawSlot) {
        return rawSlot >= 0 && rawSlot <= 44;
    }

    @Override
    public void build() {
        // Build static control slots: 45 to 52
        updateControlButtons();
        // Slot 53 depends on contents
        updateConfirmationButton();
    }

    private void updateControlButtons() {
        double multiplier = plugin.getBoosterManager().getTotalMultiplier(player);
        int bonusPercent = Math.max(0, (int) Math.round((multiplier - 1.0) * 100.0));

        // Slot 45: Book (Info & Commands)
        ItemStack book = new ItemBuilder(Material.BOOK)
                .name(" ")
                .lore(
                        " &#FF7000● &#FFFFFFБазовый коэффициент: &#FF7000+" + bonusPercent + "%",
                        "",
                        " &#00D8FF&l&n▍",
                        " &#00D8FF&l&n▍&#FFFFFF &#00D8FFКак продать предмет Скупщику?",
                        " &#00D8FF&l&n▍&#FFFFFF ",
                        " &#00D8FF&l&n▍&#FFFFFF Перенесите желаемый предмет в это",
                        " &#00D8FF&l&n▍&#FFFFFF меню и подтвердите сделку в правом",
                        " &#00D8FF&l&n▍&#FFFFFF нижнем углу меню.",
                        " &#00D8FF&l&n▍",
                        " &#00D8FF&l&n▍&#FFFFFF &#00D8FFКоманды",
                        " &#00D8FF&l&n▍&#FFFFFF ",
                        " &#00D8FF&l&n▍&#FFFFFF &#00D8FF/b auto &#FFFFFF– открыть авто-скупщик",
                        " &#00D8FF&l&n▍&#FFFFFF &#00D8FF/b items &#FFFFFF– статистика товаров",
                        " &#00D8FF&l&n▍&#FFFFFF &#00D8FF/b settings &#FFFFFF– настройки Скупщика",
                        " &#00D8FF&l&n▍&#FFFFFF &#00D8FF/b shop &#FFFFFF– магазин Скупщика",
                        " &#00D8FF&l&n▍",
                        " &#00D8FF&l&n▍&#FFFFFF Зарабатывайте монеты и очки,",
                        " &#00D8FF&l&n▍&#FFFFFF чтобы прокачивать бустеры в магазине!",
                        " &#00D8FF&l▍"
                ).build();
        setItem(45, book);

        // Slot 46: Nether Star (Auto-Seller)
        ItemStack autoSell = new ItemBuilder(Material.NETHER_STAR)
                .name(" &#FF7000❏ Авто-скупщик ❏")
                .lore(
                        "",
                        " &#FF7000&l&n▍&#FF7000 Автосдача предметов &#FFFFFFскупщику.",
                        " &#FF7000&l▍&#FFFFFF Установка фильтра на предметы.",
                        "",
                        " &#FF7000▶ &#FFFFFFНажмите, чтобы открыть"
                ).build();
        setItem(46, autoSell, e -> {
            isNavigatingAway = true;
            returnDropItems();
            new AutoSellMenu(plugin, player).open();
        });

        // Slots 47, 48, 50, 51, 52: Gray Stained Glass Pane
        ItemStack grayPane = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        setItem(47, grayPane);
        setItem(48, grayPane);
        setItem(50, grayPane);
        setItem(51, grayPane);
        setItem(52, grayPane);

        // Slot 49: Lodestone (Shop)
        ItemStack shop = new ItemBuilder(Material.LODESTONE)
                .name(" &#00D8FF❏ Магазин скупщика ❏")
                .lore(
                        "",
                        " &#00D8FF&l&n▍&#FFFFFF Раздел, где можно обменять Очки.",
                        " &#00D8FF&l▍&#FFFFFF скупщика на предметы или бустеры.",
                        "",
                        " &#00D8FF▶ &#FFFFFFНажмите, чтобы открыть"
                ).build();
        setItem(49, shop, e -> {
            isNavigatingAway = true;
            returnDropItems();
            new ShopMenu(plugin, player).open();
        });
    }

    private void updateConfirmationButton() {
        double multiplier = plugin.getBoosterManager().getTotalMultiplier(player);

        double totalBaseCoins = 0.0;
        long totalPoints = 0L;
        int sellableCount = 0;

        for (int i = 0; i <= 44; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType().isAir()) continue;

            Item item = plugin.getItemManager().findMatchingItem(stack);
            if (item != null) {
                sellableCount += stack.getAmount();
                totalBaseCoins += item.getPrice() * stack.getAmount();
                totalPoints += item.getPoints() * stack.getAmount();
            }
        }

        if (sellableCount == 0) {
            // Red pane: nothing to sell
            ItemStack redPane = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .name(" ")
                    .lore(
                            " &#FF2222✘ &#FFFFFFВы пока ничего",
                            "    &#FF2222не продаете &#FFFFFFскупщику.",
                            ""
                    ).build();
            setItem(53, redPane, null);
        } else {
            // Lime pane: confirm sell
            double totalCoins = totalBaseCoins * multiplier;
            double finalCoins = totalCoins;
            long finalPoints = totalPoints;
            int finalCount = sellableCount;

            String formattedCoins = plugin.getNumberFormatManager().formatNumber(finalCoins);
            String formattedPoints = plugin.getNumberFormatManager().formatNumber(finalPoints);
            String formattedCount = plugin.getNumberFormatManager().formatNumber(finalCount);

            ItemStack limePane = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                    .name(" ")
                    .lore(
                            " &#FF7000&n◢&#FFFFFF Награда за сдачу предметов:",
                            " &#FF7000◤&#FFC900 &#FFC900" + formattedCoins + "&#FFC900 монет ¤ &#555555| &#05FB00" + formattedPoints + "&#05FB00 очков",
                            "",
                            " &#FF7000▶ &#FFFFFFНажмите, чтобы сдать &#FF7000" + formattedCount + "&#FF7000 предметов"
                    )
                    .flags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
                    .glow()
                    .build();

            setItem(53, limePane, e -> executeSell());
        }
    }

    private synchronized void executeSell() {
        if (inventory == null) return;

        double totalBaseCoins = 0.0;
        long totalPoints = 0L;
        int count = 0;

        // Collect and clear all sellable items in slots 0-44 atomically
        for (int i = 0; i <= 44; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType().isAir()) continue;

            Item item = plugin.getItemManager().findMatchingItem(stack);
            if (item != null) {
                int amount = stack.getAmount();
                count += amount;
                totalBaseCoins += item.getPrice() * amount;
                totalPoints += item.getPoints() * amount;
                inventory.setItem(i, null);
            }
        }

        if (count <= 0) {
            updateConfirmationButton();
            player.updateInventory();
            return;
        }

        double multiplier = plugin.getBoosterManager().getTotalMultiplier(player);
        double totalCoins = totalBaseCoins * multiplier;

        // Deposit coins
        plugin.getEconomyProvider().deposit(player, totalCoins);

        // Award points
        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());
        long oldPoints = data.getPoints();
        data.addPoints(totalPoints);

        // Fire events
        Bukkit.getPluginManager().callEvent(new SellEvent(player, count, totalCoins, totalPoints));
        Bukkit.getPluginManager().callEvent(new PointsUpdateEvent(player, oldPoints, data.getPoints(), PointsUpdateEvent.Cause.SELL));

        // Sound effect
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        } catch (Throwable ignored) {}

        // Send message
        player.sendMessage(Colorizer.colorize(
                "&#00D8FF[Скупщик] &#FFFFFFВы успешно продали &e" + count + " &#FFFFFFпредметов за &a+" +
                        plugin.getNumberFormatManager().formatNumber(totalCoins) + "$ &#FFFFFFи &b+" +
                        plugin.getNumberFormatManager().formatNumber(totalPoints) + " очков&#FFFFFF!"
        ));

        // Re-evaluate confirmation button (now turns into red pane)
        updateConfirmationButton();
        player.updateInventory();
    }

    @Override
    public void onContentsChanged() {
        if (inventory == null) return;
        updateConfirmationButton();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.isShiftClick()) {
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);

            // CRITICAL SECURITY CHECK: Shift-click into drop slots is ONLY allowed when clicked inside the PLAYER'S inventory!
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(player.getInventory())) {
                player.updateInventory();
                return;
            }

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;

            // Try to place into slots 0-44 only
            ItemStack toMove = clicked.clone();
            int moved = 0;

            // 1. Stack with existing items in 0-44
            for (int i = 0; i <= 44; i++) {
                ItemStack current = inventory.getItem(i);
                if (current != null && current.isSimilar(toMove)) {
                    int space = current.getMaxStackSize() - current.getAmount();
                    if (space > 0) {
                        int add = Math.min(space, toMove.getAmount());
                        current.setAmount(current.getAmount() + add);
                        toMove.setAmount(toMove.getAmount() - add);
                        moved += add;
                        if (toMove.getAmount() <= 0) break;
                    }
                }
            }

            // 2. Place into empty slots in 0-44
            if (toMove.getAmount() > 0) {
                for (int i = 0; i <= 44; i++) {
                    ItemStack current = inventory.getItem(i);
                    if (current == null || current.getType().isAir()) {
                        inventory.setItem(i, toMove.clone());
                        moved += toMove.getAmount();
                        toMove.setAmount(0);
                        break;
                    }
                }
            }

            if (moved > 0) {
                if (toMove.getAmount() <= 0) {
                    event.setCurrentItem(null);
                } else {
                    clicked.setAmount(toMove.getAmount());
                    event.setCurrentItem(clicked);
                }
                updateConfirmationButton();
            }
            player.updateInventory();
            return;
        }

        super.handleClick(event);
    }

    private void returnDropItems() {
        if (inventory == null) return;
        boolean isDead = player.isDead() || player.getHealth() <= 0.0;
        for (int i = 0; i <= 44; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack != null && !stack.getType().isAir()) {
                inventory.setItem(i, null);
                if (isDead) {
                    player.getWorld().dropItemNaturally(player.getLocation(), stack);
                } else {
                    HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
                    for (ItemStack rem : leftovers.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), rem);
                    }
                }
            }
        }
        player.updateInventory();
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (!isNavigatingAway) {
            returnDropItems();
        }
    }
}
