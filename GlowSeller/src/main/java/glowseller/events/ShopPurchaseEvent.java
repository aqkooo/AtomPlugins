package glowseller.events;

import glowseller.models.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ShopPurchaseEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ShopItem shopItem;
    private long price;
    private boolean cancelled;

    public ShopPurchaseEvent(Player player, ShopItem shopItem, long price) {
        this.player = player;
        this.shopItem = shopItem;
        this.price = price;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return player;
    }

    public ShopItem getShopItem() {
        return shopItem;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
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
