package glowseller.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PointsUpdateEvent extends Event {
    public enum Cause {
        SELL,
        PURCHASE,
        ADMIN_GIVE,
        ADMIN_TAKE,
        ADMIN_SET,
        CUSTOM
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final long oldPoints;
    private final long newPoints;
    private final Cause cause;

    public PointsUpdateEvent(Player player, long oldPoints, long newPoints, Cause cause) {
        this.player = player;
        this.oldPoints = oldPoints;
        this.newPoints = newPoints;
        this.cause = cause;
    }

    public Player getPlayer() {
        return player;
    }

    public long getOldPoints() {
        return oldPoints;
    }

    public long getNewPoints() {
        return newPoints;
    }

    public Cause getCause() {
        return cause;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
