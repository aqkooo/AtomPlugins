package ru.atomicsqd.atomreactor.config;

/**
 * Encapsulates properties for a specific reactor upgrade level.
 */
public class ReactorLevel {

    private final int level;
    private final String name;
    private final double income;
    private final int intervalSeconds;
    private final int radius;
    private final double upgradeCost;
    private final double maxStorage;
    private final String particle;
    private final String particleColor;

    public ReactorLevel(int level, String name, double income, int intervalSeconds,
                        int radius, double upgradeCost, double maxStorage,
                        String particle, String particleColor) {
        this.level = level;
        this.name = name;
        this.income = income;
        this.intervalSeconds = Math.max(1, intervalSeconds);
        this.radius = Math.max(1, radius);
        this.upgradeCost = upgradeCost;
        this.maxStorage = Math.max(1.0, maxStorage);
        this.particle = particle != null ? particle : "DUST";
        this.particleColor = particleColor != null ? particleColor : "#56CCF2";
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public double getIncome() {
        return income;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public int getRadius() {
        return radius;
    }

    public double getUpgradeCost() {
        return upgradeCost;
    }

    public double getMaxStorage() {
        return maxStorage;
    }

    public String getParticle() {
        return particle;
    }

    public String getParticleColor() {
        return particleColor;
    }
}
