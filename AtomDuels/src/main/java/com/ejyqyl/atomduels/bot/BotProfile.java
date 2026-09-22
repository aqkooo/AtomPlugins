package com.ejyqyl.atomduels.bot;

/**
 * Combat attributes and behavioral parameters of a PvP Duel Bot.
 * Authored by ejyqyl.
 */
public class BotProfile {

    private BotDifficulty difficulty;
    private int reactionTimeMs;
    private int attackDelayTicks;
    private double aimAccuracy;
    private double strafeChance;
    private double shieldBlockChance;
    private double potionHealChance;
    private double pearlClutchChance;
    private double totemHoldChance;

    public BotProfile(BotDifficulty difficulty) {
        this.difficulty = difficulty;
        this.reactionTimeMs = difficulty.getReactionTimeMs();
        this.attackDelayTicks = difficulty.getAttackDelayTicks();
        this.aimAccuracy = difficulty.getAimAccuracy();
        this.strafeChance = difficulty.getStrafeChance();
        this.shieldBlockChance = difficulty.getShieldBlockChance();
        this.potionHealChance = difficulty.getPotionHealChance();
        this.pearlClutchChance = difficulty.getPearlClutchChance();
        this.totemHoldChance = difficulty.getTotemHoldChance();
    }

    public BotDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(BotDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public int getReactionTimeMs() {
        return reactionTimeMs;
    }

    public void setReactionTimeMs(int reactionTimeMs) {
        this.reactionTimeMs = reactionTimeMs;
    }

    public int getAttackDelayTicks() {
        return attackDelayTicks;
    }

    public void setAttackDelayTicks(int attackDelayTicks) {
        this.attackDelayTicks = attackDelayTicks;
    }

    public double getAimAccuracy() {
        return aimAccuracy;
    }

    public void setAimAccuracy(double aimAccuracy) {
        this.aimAccuracy = aimAccuracy;
    }

    public double getStrafeChance() {
        return strafeChance;
    }

    public void setStrafeChance(double strafeChance) {
        this.strafeChance = strafeChance;
    }

    public double getShieldBlockChance() {
        return shieldBlockChance;
    }

    public void setShieldBlockChance(double shieldBlockChance) {
        this.shieldBlockChance = shieldBlockChance;
    }

    public double getPotionHealChance() {
        return potionHealChance;
    }

    public void setPotionHealChance(double potionHealChance) {
        this.potionHealChance = potionHealChance;
    }

    public double getPearlClutchChance() {
        return pearlClutchChance;
    }

    public void setPearlClutchChance(double pearlClutchChance) {
        this.pearlClutchChance = pearlClutchChance;
    }

    public double getTotemHoldChance() {
        return totemHoldChance;
    }

    public void setTotemHoldChance(double totemHoldChance) {
        this.totemHoldChance = totemHoldChance;
    }
}
