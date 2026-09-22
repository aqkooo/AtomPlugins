package com.ejyqyl.atompvpbot.ai;

import com.ejyqyl.atompvpbot.config.ConfigManager;

/**
 * Custom configurable AI profile clamped to defined bounds.
 *
 * @author ejyqyl
 */
public class CustomAIProfile {

    private int cps = 10;
    private double reach = 3.0;
    private float aimSpeed = 20.0f;
    private double aimAccuracy = 0.85;
    private double shieldBlockChance = 0.50;
    private boolean sprintResetCombo = true;
    private int comboMaxHits = 3;
    private double jumpCritChance = 0.50;
    private boolean axeShieldBreak = true;
    private boolean maceSmash = true;
    private boolean pearlClutch = true;
    private boolean potionUse = true;
    private long reactionMs = 250;

    public CustomAIProfile() {}

    public CustomAIProfile(ConfigManager config) {
        if (config == null) return;
        this.cps = (int) Math.round(config.getCustomDefault("cps", 10));
        this.reach = config.getCustomDefault("reach", 3.0);
        this.aimSpeed = (float) config.getCustomDefault("aim-speed", 20.0);
        this.aimAccuracy = config.getCustomDefault("aim-accuracy", 0.85);
        this.shieldBlockChance = config.getCustomDefault("shield-block-chance", 0.50);
        this.sprintResetCombo = true;
        this.comboMaxHits = (int) Math.round(config.getCustomDefault("combo-max-hits", 3));
        this.jumpCritChance = config.getCustomDefault("jump-crit-chance", 0.50);
        this.axeShieldBreak = true;
        this.maceSmash = true;
        this.pearlClutch = true;
        this.potionUse = true;
        this.reactionMs = (long) config.getCustomDefault("reaction-ms", 250);
    }

    public void clamp(ConfigManager config) {
        if (config == null) return;
        this.cps = (int) Math.clamp(cps, (int) config.getCustomMin("cps", 1), (int) config.getCustomMax("cps", 20));
        this.reach = Math.clamp(reach, config.getCustomMin("reach", 1.5), config.getCustomMax("reach", 4.5));
        this.aimSpeed = (float) Math.clamp(aimSpeed, config.getCustomMin("aim-speed", 1.0), config.getCustomMax("aim-speed", 50.0));
        this.aimAccuracy = Math.clamp(aimAccuracy, config.getCustomMin("aim-accuracy", 0.1), config.getCustomMax("aim-accuracy", 1.0));
        this.shieldBlockChance = Math.clamp(shieldBlockChance, config.getCustomMin("shield-block-chance", 0.0), config.getCustomMax("shield-block-chance", 1.0));
        this.comboMaxHits = (int) Math.clamp(comboMaxHits, (int) config.getCustomMin("combo-max-hits", 1), (int) config.getCustomMax("combo-max-hits", 10));
        this.jumpCritChance = Math.clamp(jumpCritChance, config.getCustomMin("jump-crit-chance", 0.0), config.getCustomMax("jump-crit-chance", 1.0));
        this.reactionMs = (long) Math.clamp(reactionMs, config.getCustomMin("reaction-ms", 50), config.getCustomMax("reaction-ms", 1000));
    }

    public int getCps() { return cps; }
    public void setCps(int cps) { this.cps = cps; }

    public double getReach() { return reach; }
    public void setReach(double reach) { this.reach = reach; }

    public float getAimSpeed() { return aimSpeed; }
    public void setAimSpeed(float aimSpeed) { this.aimSpeed = aimSpeed; }

    public double getAimAccuracy() { return aimAccuracy; }
    public void setAimAccuracy(double aimAccuracy) { this.aimAccuracy = aimAccuracy; }

    public double getShieldBlockChance() { return shieldBlockChance; }
    public void setShieldBlockChance(double shieldBlockChance) { this.shieldBlockChance = shieldBlockChance; }

    public boolean isSprintResetCombo() { return sprintResetCombo; }
    public void setSprintResetCombo(boolean sprintResetCombo) { this.sprintResetCombo = sprintResetCombo; }

    public int getComboMaxHits() { return comboMaxHits; }
    public void setComboMaxHits(int comboMaxHits) { this.comboMaxHits = comboMaxHits; }

    public double getJumpCritChance() { return jumpCritChance; }
    public void setJumpCritChance(double jumpCritChance) { this.jumpCritChance = jumpCritChance; }

    public boolean isAxeShieldBreak() { return axeShieldBreak; }
    public void setAxeShieldBreak(boolean axeShieldBreak) { this.axeShieldBreak = axeShieldBreak; }

    public boolean isMaceSmash() { return maceSmash; }
    public void setMaceSmash(boolean maceSmash) { this.maceSmash = maceSmash; }

    public boolean isPearlClutch() { return pearlClutch; }
    public void setPearlClutch(boolean pearlClutch) { this.pearlClutch = pearlClutch; }

    public boolean isPotionUse() { return potionUse; }
    public void setPotionUse(boolean potionUse) { this.potionUse = potionUse; }

    public long getReactionMs() { return reactionMs; }
    public void setReactionMs(long reactionMs) { this.reactionMs = reactionMs; }
}
