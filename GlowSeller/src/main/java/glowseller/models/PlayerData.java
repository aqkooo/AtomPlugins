package glowseller.models;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {
    public enum AutoSellMode {
        OFF,
        ENABLED,
        ENABLED_SESSION
    }

    private final UUID uuid;
    private long points;
    private String activeBoosterKey;
    private long boosterExpireAt;
    private AutoSellMode autoSellMode = AutoSellMode.OFF;
    private final java.util.Set<String> autoSellFilter = ConcurrentHashMap.newKeySet();
    private volatile boolean dirty;

    // Cache of purchases for instant, non-blocking limit checks
    private final Map<String, Integer> purchaseCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastPurchaseTimes = new ConcurrentHashMap<>();

    public PlayerData(UUID uuid) {
        this(uuid, 0L, null, 0L, AutoSellMode.OFF, null);
    }

    public PlayerData(UUID uuid, long points, String activeBoosterKey, long boosterExpireAt) {
        this(uuid, points, activeBoosterKey, boosterExpireAt, AutoSellMode.OFF, null);
    }

    public PlayerData(UUID uuid, long points, String activeBoosterKey, long boosterExpireAt,
                      AutoSellMode autoSellMode, java.util.Collection<String> filter) {
        this.uuid = uuid;
        this.points = points;
        this.activeBoosterKey = activeBoosterKey;
        this.boosterExpireAt = boosterExpireAt;
        this.autoSellMode = (autoSellMode != null) ? autoSellMode : AutoSellMode.OFF;
        if (filter != null) {
            for (String k : filter) {
                if (k != null && !k.isEmpty()) {
                    this.autoSellFilter.add(k.toUpperCase());
                }
            }
        }
        this.dirty = false;
    }

    public UUID getUuid() {
        return uuid;
    }

    public synchronized long getPoints() {
        return points;
    }

    public synchronized void setPoints(long points) {
        this.points = Math.max(0L, points);
        this.dirty = true;
    }

    public synchronized void addPoints(long amount) {
        if (amount > 0) {
            this.points += amount;
            this.dirty = true;
        }
    }

    public synchronized boolean takePoints(long amount) {
        if (amount <= 0) return true;
        if (this.points >= amount) {
            this.points -= amount;
            this.dirty = true;
            return true;
        }
        return false;
    }

    public synchronized String getActiveBoosterKey() {
        return activeBoosterKey;
    }

    public synchronized void setActiveBoosterKey(String activeBoosterKey) {
        this.activeBoosterKey = activeBoosterKey;
        this.dirty = true;
    }

    public synchronized long getBoosterExpireAt() {
        return boosterExpireAt;
    }

    public synchronized void setBoosterExpireAt(long boosterExpireAt) {
        this.boosterExpireAt = boosterExpireAt;
        this.dirty = true;
    }

    public synchronized boolean hasActiveBooster() {
        return activeBoosterKey != null && boosterExpireAt > System.currentTimeMillis();
    }

    public synchronized long getBoosterTimeLeftSeconds() {
        if (!hasActiveBooster()) return 0L;
        long diff = boosterExpireAt - System.currentTimeMillis();
        return Math.max(0L, diff / 1000L);
    }

    public synchronized void clearBooster() {
        this.activeBoosterKey = null;
        this.boosterExpireAt = 0L;
        this.dirty = true;
    }

    public synchronized AutoSellMode getAutoSellMode() {
        return autoSellMode != null ? autoSellMode : AutoSellMode.OFF;
    }

    public synchronized void setAutoSellMode(AutoSellMode mode) {
        this.autoSellMode = (mode != null) ? mode : AutoSellMode.OFF;
        this.dirty = true;
    }

    public synchronized boolean isAutoSellActive() {
        return autoSellMode == AutoSellMode.ENABLED || autoSellMode == AutoSellMode.ENABLED_SESSION;
    }

    public boolean isItemAutoSellEnabled(String itemKey) {
        if (itemKey == null) return false;
        return autoSellFilter.contains(itemKey.toUpperCase());
    }

    public synchronized void toggleItemAutoSell(String itemKey) {
        if (itemKey == null) return;
        String key = itemKey.toUpperCase();
        if (autoSellFilter.contains(key)) {
            autoSellFilter.remove(key);
        } else {
            autoSellFilter.add(key);
        }
        this.dirty = true;
    }

    public synchronized void setItemAutoSell(String itemKey, boolean enabled) {
        if (itemKey == null) return;
        String key = itemKey.toUpperCase();
        if (enabled) {
            autoSellFilter.add(key);
        } else {
            autoSellFilter.remove(key);
        }
        this.dirty = true;
    }

    public java.util.Set<String> getAutoSellFilter() {
        return java.util.Collections.unmodifiableSet(autoSellFilter);
    }

    public synchronized void setAutoSellFilter(java.util.Collection<String> keys) {
        autoSellFilter.clear();
        if (keys != null) {
            for (String k : keys) {
                if (k != null && !k.isEmpty()) autoSellFilter.add(k.toUpperCase());
            }
        }
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void recordPurchase(String itemKey, long timestamp) {
        if (itemKey == null) return;
        purchaseCounts.merge(itemKey, 1, Integer::sum);
        lastPurchaseTimes.put(itemKey, timestamp);
    }

    public void initPurchaseHistory(Map<String, Integer> counts, Map<String, Long> lastTimes) {
        if (counts != null) this.purchaseCounts.putAll(counts);
        if (lastTimes != null) this.lastPurchaseTimes.putAll(lastTimes);
    }

    public int getPurchaseCount(String itemKey) {
        return purchaseCounts.getOrDefault(itemKey, 0);
    }

    public boolean hasReachedLimit(String itemKey, String limit) {
        if (limit == null || limit.isEmpty() || "unlimited".equalsIgnoreCase(limit)) {
            return false;
        }
        if ("once".equalsIgnoreCase(limit)) {
            return getPurchaseCount(itemKey) > 0;
        }
        if ("daily".equalsIgnoreCase(limit)) {
            Long last = lastPurchaseTimes.get(itemKey);
            if (last == null) return false;
            return (System.currentTimeMillis() - last) < 86400000L;
        }
        try {
            int maxCount = Integer.parseInt(limit.trim());
            return getPurchaseCount(itemKey) >= maxCount;
        } catch (NumberFormatException ignored) {}
        return false;
    }
}
