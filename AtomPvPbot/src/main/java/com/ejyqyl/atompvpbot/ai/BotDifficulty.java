package com.ejyqyl.atompvpbot.ai;

/**
 * Difficulty levels for AtomPvPbot.
 *
 * @author ejyqyl
 */
public enum BotDifficulty {

    EASY("<gradient:#55FF55:#00AA00>Легкий</gradient>", 5, 2.7, 8.0f, 0.65, 0.25, false, 1, 0.2, false, false, false, false, 650),
    NORMAL("<gradient:#FFFF55:#FFAA00>Нормальный</gradient>", 8, 2.9, 14.0f, 0.82, 0.50, true, 2, 0.45, true, false, false, true, 380),
    HARD("<gradient:#FF5555:#AA0000>Сложный</gradient>", 11, 3.0, 22.0f, 0.92, 0.75, true, 4, 0.70, true, true, true, true, 180),
    EXPERT("<gradient:#FF0055:#880022>Эксперт</gradient>", 14, 3.1, 35.0f, 0.98, 0.90, true, 6, 0.90, true, true, true, true, 75),
    CUSTOM("<gradient:#AA00AA:#5500AA>Кастомный</gradient>", 9, 2.9, 16.0f, 0.85, 0.50, true, 3, 0.50, true, true, true, true, 300);

    private final String displayName;
    private final int cps;
    private final double reach;
    private final float aimSpeed;
    private final double aimAccuracy;
    private final double shieldBlockChance;
    private final boolean sprintResetCombo;
    private final int comboMaxHits;
    private final double jumpCritChance;
    private final boolean axeShieldBreak;
    private final boolean maceSmash;
    private final boolean pearlClutch;
    private final boolean potionUse;
    private final long reactionMs;

    BotDifficulty(String displayName, int cps, double reach, float aimSpeed, double aimAccuracy,
                  double shieldBlockChance, boolean sprintResetCombo, int comboMaxHits,
                  double jumpCritChance, boolean axeShieldBreak, boolean maceSmash,
                  boolean pearlClutch, boolean potionUse, long reactionMs) {
        this.displayName = displayName;
        this.cps = cps;
        this.reach = reach;
        this.aimSpeed = aimSpeed;
        this.aimAccuracy = aimAccuracy;
        this.shieldBlockChance = shieldBlockChance;
        this.sprintResetCombo = sprintResetCombo;
        this.comboMaxHits = comboMaxHits;
        this.jumpCritChance = jumpCritChance;
        this.axeShieldBreak = axeShieldBreak;
        this.maceSmash = maceSmash;
        this.pearlClutch = pearlClutch;
        this.potionUse = potionUse;
        this.reactionMs = reactionMs;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCps() {
        return cps;
    }

    public double getReach() {
        return reach;
    }

    public float getAimSpeed() {
        return aimSpeed;
    }

    public double getAimAccuracy() {
        return aimAccuracy;
    }

    public double getShieldBlockChance() {
        return shieldBlockChance;
    }

    public boolean isSprintResetCombo() {
        return sprintResetCombo;
    }

    public int getComboMaxHits() {
        return comboMaxHits;
    }

    public double getJumpCritChance() {
        return jumpCritChance;
    }

    public boolean isAxeShieldBreak() {
        return axeShieldBreak;
    }

    public boolean isMaceSmash() {
        return maceSmash;
    }

    public boolean isPearlClutch() {
        return pearlClutch;
    }

    public boolean isPotionUse() {
        return potionUse;
    }

    public long getReactionMs() {
        return reactionMs;
    }

    public static BotDifficulty fromString(String name) {
        if (name == null) return NORMAL;
        for (BotDifficulty diff : values()) {
            if (diff.name().equalsIgnoreCase(name)) {
                return diff;
            }
        }
        return NORMAL;
    }
}
