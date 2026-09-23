package glowseller.managers;

import glowseller.Main;
import glowseller.economy.EconomyProvider;
import glowseller.events.PointsUpdateEvent;
import glowseller.events.SellEvent;
import glowseller.models.PlayerData;
import glowseller.models.item.Item;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

public class SellManager {
    private final Main plugin;

    public SellManager(Main plugin) {
        this.plugin = plugin;
    }

    public synchronized boolean sellInventory(Player player) {
        if (player == null || !player.isOnline()) return false;

        if (plugin.getPlayerDataCache().isLoading(player.getUniqueId())) {
            player.sendMessage("§eПодождите, ваши данные еще загружаются...");
            playErrorSound(player);
            return false;
        }

        PlayerInventory inv = player.getInventory();
        List<ItemStack> toSell = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();

        double baseCoins = 0.0;
        long earnedPoints = 0L;

        // Iterate standard storage slots 0-35
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) continue;

            Item item = plugin.getItemManager().findMatchingItem(stack);
            if (item != null) {
                toSell.add(stack.clone());
                slots.add(i);
                baseCoins += item.getPrice() * stack.getAmount();
                earnedPoints += item.getPoints() * stack.getAmount();
            }
        }

        if (toSell.isEmpty()) {
            plugin.getMessageConfig().send(player, "sell.no_items");
            playErrorSound(player);
            return false;
        }

        double multiplier = plugin.getBoosterManager().getTotalMultiplier(player);
        double earnedCoins = baseCoins * multiplier;

        SellEvent sellEvent = new SellEvent(player, toSell, earnedCoins, earnedPoints, multiplier);
        Bukkit.getPluginManager().callEvent(sellEvent);
        if (sellEvent.isCancelled()) {
            return false;
        }

        earnedCoins = sellEvent.getEarnedCoins();
        earnedPoints = sellEvent.getEarnedPoints();

        // 1. Remove items first to prevent dupes
        for (int slot : slots) {
            inv.setItem(slot, null);
        }

        // 2. Deposit currency via economy provider
        EconomyProvider eco = plugin.getEconomyProvider();
        if (earnedCoins > 0 && eco != null && !eco.deposit(player, earnedCoins)) {
            // Rollback items to exact original slots if deposit failed
            for (int i = 0; i < slots.size(); i++) {
                inv.setItem(slots.get(i), toSell.get(i));
            }
            plugin.getMessageConfig().send(player, "sell.economy_error");
            playErrorSound(player);
            return false;
        }

        // 3. Add points
        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());
        long oldPoints = data.getPoints();
        data.addPoints(earnedPoints);

        Bukkit.getPluginManager().callEvent(new PointsUpdateEvent(player, oldPoints, data.getPoints(), PointsUpdateEvent.Cause.SELL));

        // Format and send message
        String coinsFormatted = plugin.getNumberFormatManager().formatNumber(earnedCoins);
        String pointsFormatted = plugin.getNumberFormatManager().formatNumber(earnedPoints);
        String multFormatted = String.format("%.2f", multiplier);

        plugin.getMessageConfig().send(player, "sell.success",
                "%coins%", coinsFormatted,
                "%points%", pointsFormatted,
                "%multiplier%", multFormatted,
                "%count%", String.valueOf(toSell.size())
        );

        playSellSound(player);
        return true;
    }

    private void playSellSound(Player player) {
        String soundName = plugin.getMainConfig().getSellSound();
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
