package glowseller.tasks;

import glowseller.Main;
import glowseller.events.PointsUpdateEvent;
import glowseller.events.SellEvent;
import glowseller.models.PlayerData;
import glowseller.models.item.Item;
import glowseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoSellTask extends BukkitRunnable {
    private final Main plugin;

    public AutoSellTask(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) continue;

            PlayerData data = plugin.getPlayerDataCache().get(player.getUniqueId());
            if (data == null || !data.isAutoSellActive()) continue;

            // Don't auto-sell while player has the interactive sell menu open
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof glowseller.menu.SellMenu) {
                continue;
            }

            PlayerInventory inv = player.getInventory();
            double totalBaseCoins = 0.0;
            long totalPoints = 0L;
            int totalItemsSold = 0;

            for (int slot = 0; slot < 36; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (stack == null || stack.getType().isAir()) continue;

                Item item = plugin.getItemManager().findMatchingItem(stack);
                if (item == null) continue;

                if (!data.isItemAutoSellEnabled(item.getId())) continue;

                int amount = stack.getAmount();
                totalItemsSold += amount;
                totalBaseCoins += item.getPrice() * amount;
                totalPoints += item.getPoints() * amount;
                inv.setItem(slot, null);
            }

            if (totalItemsSold > 0) {
                double multiplier = plugin.getBoosterManager().getTotalMultiplier(player);
                double totalCoins = totalBaseCoins * multiplier;

                plugin.getEconomyProvider().deposit(player, totalCoins);
                long oldPoints = data.getPoints();
                data.addPoints(totalPoints);

                Bukkit.getPluginManager().callEvent(new SellEvent(player, totalItemsSold, totalCoins, totalPoints));
                Bukkit.getPluginManager().callEvent(new PointsUpdateEvent(player, oldPoints, data.getPoints(), PointsUpdateEvent.Cause.SELL));

                player.updateInventory();

                try {
                    player.sendActionBar(Colorizer.colorize(
                            "&#FF7000[Авто-скупщик] &#FFFFFFПродано &e" + totalItemsSold +
                                    " &#FFFFFFпредметов: &a+" + plugin.getNumberFormatManager().formatNumber(totalCoins) +
                                    "$ &#FFFFFFи &b+" + plugin.getNumberFormatManager().formatNumber(totalPoints) + " очков"
                    ));
                } catch (Throwable ignored) {}
            }
        }
    }
}
