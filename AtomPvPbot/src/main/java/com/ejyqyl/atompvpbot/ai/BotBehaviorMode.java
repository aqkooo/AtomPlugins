package com.ejyqyl.atompvpbot.ai;

/**
 * Tactical behavior mode of AtomPvPbot.
 *
 * @author ejyqyl
 */
public enum BotBehaviorMode {

    AGGRESSIVE(
            "<gradient:#FF4B2B:#FF416C>Агрессивный</gradient>",
            "Постоянное преследование, прессинг, jump-криты и минимальное отступление.",
            1.15, // Speed multiplier
            false // Doesn't retreat when low on HP
    ),
    DEFENSIVE(
            "<gradient:#36D1DC:#5B86E5>Оборонительный</gradient>",
            "Дистанционный бой, блокирование щитом, контратаки и своевременное лечение.",
            0.95,
            true
    ),
    STRAFE(
            "<gradient:#F7971E:#FFD200>Стрейф</gradient>",
            "Ортогональное кружение вокруг игрока, комбо-удары с флангов и уклонение.",
            1.05,
            false
    ),
    BALANCED(
            "<gradient:#00B5FD:#7670E5>Сбалансированный</gradient>",
            "Адаптивный бой, сбалансированные атаки, защита и тактическое сближение.",
            1.0,
            true
    );

    private final String displayName;
    private final String description;
    private final double speedMultiplier;
    private final boolean retreatOnLowHp;

    BotBehaviorMode(String displayName, String description, double speedMultiplier, boolean retreatOnLowHp) {
        this.displayName = displayName;
        this.description = description;
        this.speedMultiplier = speedMultiplier;
        this.retreatOnLowHp = retreatOnLowHp;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public boolean isRetreatOnLowHp() {
        return retreatOnLowHp;
    }

    public static BotBehaviorMode fromString(String name) {
        if (name == null) return BALANCED;
        for (BotBehaviorMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return BALANCED;
    }
}
