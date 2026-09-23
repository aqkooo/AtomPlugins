package glowseller.managers;

import glowseller.Main;
import glowseller.events.PointsUpdateEvent;
import glowseller.events.ShopPurchaseEvent;
import glowseller.models.PlayerData;
import glowseller.models.ShopItem;
import glowseller.utils.ItemBuilder;
import glowseller.utils.ModelDataUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopManager {
    private final Main plugin;

    public ShopManager(Main plugin) {
        this.plugin = plugin;
    }

    public synchronized boolean purchase(Player player, String itemKey) {
        if (player == null || itemKey == null) return false;

        if (plugin.getPlayerDataCache().isLoading(player.getUniqueId())) {
            player.sendMessage("§eПодождите, ваши данные еще загружаются...");
            playErrorSound(player);
            return false;
        }

        ShopItem item = plugin.getShopConfig().getItem(itemKey);
        if (item == null) {
            plugin.getMessageConfig().send(player, "shop.item_not_found");
            playErrorSound(player);
            return false;
        }

        if (item.getPermission() != null && !item.getPermission().isEmpty()) {
            if (!player.hasPermission(item.getPermission())) {
                plugin.getMessageConfig().send(player, "shop.no_permission");
                playErrorSound(player);
                return false;
            }
        }

        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());
        long cost = item.getPrice();

        if (data.getPoints() < cost) {
            plugin.getMessageConfig().send(player, "shop.not_enough_points",
                    "%need%", plugin.getNumberFormatManager().formatNumber(cost - data.getPoints()),
                    "%price%", plugin.getNumberFormatManager().formatNumber(cost),
                    "%points%", plugin.getNumberFormatManager().formatNumber(data.getPoints())
            );
            playErrorSound(player);
            return false;
        }

        if (item.getType() == ShopItem.Type.BOOSTER) {
            if (data.hasActiveBooster() || !plugin.getBoosterManager().canApplyBooster(player, item.getKey())) {
                plugin.getMessageConfig().send(player, "shop.booster_already_active");
                playErrorSound(player);
                return false;
            }
        }

        // Limit check using in-memory cached history (non-blocking, instant, dupe-safe)
        if (data.hasReachedLimit(item.getKey(), item.getLimit())) {
            plugin.getMessageConfig().send(player, "shop.limit_reached");
            playErrorSound(player);
            return false;
        }

        ShopPurchaseEvent purchaseEvent = new ShopPurchaseEvent(player, item, cost);
        Bukkit.getPluginManager().callEvent(purchaseEvent);
        if (purchaseEvent.isCancelled()) {
            return false;
        }

        cost = purchaseEvent.getPrice();
        if (cost < 0) {
            return false;
        }

        long oldPoints = data.getPoints();
        if (!data.takePoints(cost)) {
            plugin.getMessageConfig().send(player, "shop.not_enough_points");
            playErrorSound(player);
            return false;
        }

        // Record purchase in memory immediately to prevent multi-click bypass
        long now = System.currentTimeMillis();
        data.recordPurchase(item.getKey(), now);

        // Give reward
        switch (item.getType()) {
            case BOOSTER -> {
                plugin.getBoosterManager().applyBooster(player, item.getKey(), item.getMultiplier(), item.getDurationSeconds());
            }
            case ITEM -> {
                ItemStack stack = new ItemStack(item.getMaterial(), Math.max(1, item.getGiveAmount()));
                ItemBuilder ib = new ItemBuilder(stack);
                if (item.getName() != null && !item.getName().isEmpty()) {
                    ib.name(item.getName());
                }
                if (item.getLore() != null && !item.getLore().isEmpty()) {
                    List<String> giveLore = new ArrayList<>();
                    for (String line : item.getLore()) {
                        if (!line.contains("Купить за") && !line.contains("▶")) {
                            giveLore.add(line);
                        }
                    }
                    ib.lore(giveLore);
                }
                if (item.getCustomModelData() != null) {
                    ib.customModelData(item.getCustomModelData());
                }
                Map<Integer, ItemStack> left = player.getInventory().addItem(ib.build());
                left.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            }
            case COMMAND -> {
                for (String cmd : item.getCommands()) {
                    if (cmd == null || cmd.isEmpty()) continue;
                    String formatted = cmd.replace("%player%", player.getName()).trim();
                    if (formatted.startsWith("/")) {
                        formatted = formatted.substring(1);
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatted);
                }
            }
            default -> {}
        }

        // Log purchase async to DB
        final long finalCost = cost;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getPurchaseLogRepository().logPurchase(
                    player.getUniqueId(),
                    player.getName(),
                    item.getKey(),
                    item.getCategory(),
                    finalCost,
                    now
            );
        });

        Bukkit.getPluginManager().callEvent(new PointsUpdateEvent(player, oldPoints, data.getPoints(), PointsUpdateEvent.Cause.PURCHASE));

        plugin.getMessageConfig().send(player, "shop.purchase_success",
                "%item%", item.getName(),
                "%price%", plugin.getNumberFormatManager().formatNumber(cost),
                "%points%", plugin.getNumberFormatManager().formatNumber(data.getPoints())
        );

        playBuySound(player);
        return true;
    }

    private void playBuySound(Player player) {
        String soundName = plugin.getMainConfig().getBuySound();
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {}
    }

    private void playErrorSound(Player player) {
        String soundName = plugin.getMainConfig().getErrorSound();
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {}
    }
}
