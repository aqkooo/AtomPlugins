package glowseller.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class SellEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final List<ItemStack> soldItems;
    private double earnedCoins;
    private long earnedPoints;
    private double multiplier;
    private boolean cancelled;

    public SellEvent(Player player, List<ItemStack> soldItems, double earnedCoins, long earnedPoints, double multiplier) {
        this.player = player;
        this.soldItems = soldItems != null ? Collections.unmodifiableList(soldItems) : Collections.emptyList();
        this.earnedCoins = earnedCoins;
        this.earnedPoints = earnedPoints;
        this.multiplier = multiplier;
        this.cancelled = false;
    }

    public SellEvent(Player player, int itemCount, double earnedCoins, long earnedPoints) {
        this(player, Collections.emptyList(), earnedCoins, earnedPoints, 1.0);
    }

    public Player getPlayer() {
        return player;
    }

    public List<ItemStack> getSoldItems() {
        return soldItems;
    }

    public double getEarnedCoins() {
        return earnedCoins;
    }

    public void setEarnedCoins(double earnedCoins) {
        this.earnedCoins = earnedCoins;
    }

    public long getEarnedPoints() {
        return earnedPoints;
    }

    public void setEarnedPoints(long earnedPoints) {
        this.earnedPoints = earnedPoints;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
