package ru.atomicsqd.glowtrade.model;

import org.bukkit.scheduler.BukkitTask;
import java.util.UUID;

/**
 * Модель запроса на обмен между двумя игроками.
 */
public class TradeRequest {

    private final UUID sender;
    private final UUID target;
    private final long createdAt;
    private BukkitTask expiryTask;

    public TradeRequest(UUID sender, UUID target) {
        this.sender = sender;
        this.target = target;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getTarget() {
        return target;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(int timeoutSeconds) {
        return System.currentTimeMillis() - createdAt > (timeoutSeconds * 1000L);
    }

    public void setExpiryTask(BukkitTask task) {
        if (this.expiryTask != null) {
            this.expiryTask.cancel();
        }
        this.expiryTask = task;
    }

    public void cancelExpiryTask() {
        if (this.expiryTask != null) {
            this.expiryTask.cancel();
            this.expiryTask = null;
        }
    }
}
