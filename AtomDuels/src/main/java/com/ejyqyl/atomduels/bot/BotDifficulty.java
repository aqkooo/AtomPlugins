package com.ejyqyl.atomduels.bot;

/**
 * Predefined bot difficulty tiers with distinct combat capabilities.
 * Authored by ejyqyl.
 */
public enum BotDifficulty {
    EASY("Легкий", "<#55FF55>", 700, 18, 0.65, 0.20, 0.15, 0.30, 0.05, 0.20),
    NORMAL("Нормальный", "<#FFFF55>", 450, 12, 0.80, 0.45, 0.40, 0.60, 0.20, 0.50),
    HARD("Сложный", "<#FFAA00>", 250, 9, 0.90, 0.70, 0.65, 0.80, 0.45, 0.80),
    EXPERT("Эксперт", "<#FF5555>", 120, 7, 0.98, 0.90, 0.85, 0.95, 0.75, 0.95),
    CUSTOM("Пользовательский", "<#A335EE>", 300, 10, 0.85, 0.50, 0.50, 0.50, 0.30, 0.50);

    private final String displayName;
    private final String colorTag;
    private final int reactionTimeMs;
    private final int attackDelayTicks;
    private final double aimAccuracy;
    private final double strafeChance;
    private final double shieldBlockChance;
    private final double potionHealChance;
    private final double pearlClutchChance;
    private final double totemHoldChance;

    BotDifficulty(String displayName, String colorTag, int reactionTimeMs, int attackDelayTicks,
                  double aimAccuracy, double strafeChance, double shieldBlockChance,
                  double potionHealChance, double pearlClutchChance, double totemHoldChance) {
        this.displayName = displayName;
        this.colorTag = colorTag;
        this.reactionTimeMs = reactionTimeMs;
        this.attackDelayTicks = attackDelayTicks;
        this.aimAccuracy = aimAccuracy;
        this.strafeChance = strafeChance;
        this.shieldBlockChance = shieldBlockChance;
        this.potionHealChance = potionHealChance;
        this.pearlClutchChance = pearlClutchChance;
        this.totemHoldChance = totemHoldChance;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorTag() {
        return colorTag;
    }

    public int getReactionTimeMs() {
        return reactionTimeMs;
    }

    public int getAttackDelayTicks() {
        return attackDelayTicks;
    }

    public double getAimAccuracy() {
        return aimAccuracy;
    }

    public double getStrafeChance() {
        return strafeChance;
    }

    public double getShieldBlockChance() {
        return shieldBlockChance;
    }

    public double getPotionHealChance() {
        return potionHealChance;
    }

    public double getPearlClutchChance() {
        return pearlClutchChance;
    }

    public double getTotemHoldChance() {
        return totemHoldChance;
    }

    public String getFormattedName() {
        return colorTag + displayName + "</" + colorTag.replace("<#", "#").replace(">", "") + ">";
    }
}
