package glowseller.models;

public class ActiveBooster {
    private final String key;
    private final double multiplier;
    private final long expireAt;

    public ActiveBooster(String key, double multiplier, long expireAt) {
        this.key = key;
        this.multiplier = multiplier;
        this.expireAt = expireAt;
    }

    public String getKey() {
        return key;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expireAt;
    }

    public long getRemainingSeconds() {
        if (isExpired()) return 0L;
        return (expireAt - System.currentTimeMillis()) / 1000L;
    }
}
