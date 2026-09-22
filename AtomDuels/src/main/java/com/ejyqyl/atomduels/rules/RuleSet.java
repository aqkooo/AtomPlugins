package com.ejyqyl.atomduels.rules;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Encapsulates the battle conditions, rules, rounds, kit, and stakes for a duel.
 * Authored by ejyqyl.
 */
public class RuleSet {

    public enum DuelMode {
        RANKED("Рейтинговый", true, false),
        CASUAL("Обычный", false, false),
        OWN_ITEMS("Со своими вещами", false, true);

        private final String displayName;
        private final boolean changesElo;
        private final boolean dropInventoryOnDeath;

        DuelMode(String displayName, boolean changesElo, boolean dropInventoryOnDeath) {
            this.displayName = displayName;
            this.changesElo = changesElo;
            this.dropInventoryOnDeath = dropInventoryOnDeath;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isChangesElo() {
            return changesElo;
        }

        public boolean isDropInventoryOnDeath() {
            return dropInventoryOnDeath;
        }
    }

    private final Map<Rule, Boolean> rules = new EnumMap<>(Rule.class);
    private DuelMode mode = DuelMode.RANKED;
    private int maxRounds = 1; // 1 (Bo1), 3 (Bo3), 5 (Bo5)
    private double betAmount = 0.0;
    private String kitName = "Classic";
    private String arenaName = null; // null means any free arena

    public RuleSet() {
        for (Rule rule : Rule.values()) {
            rules.put(rule, rule.isDefaultEnabled());
        }
    }

    public RuleSet(RuleSet other) {
        this.rules.putAll(other.rules);
        this.mode = other.mode;
        this.maxRounds = other.maxRounds;
        this.betAmount = other.betAmount;
        this.kitName = other.kitName;
        this.arenaName = other.arenaName;
    }

    public boolean isRuleEnabled(Rule rule) {
        return rules.getOrDefault(rule, rule.isDefaultEnabled());
    }

    public void setRule(Rule rule, boolean enabled) {
        rules.put(rule, enabled);
    }

    public void toggleRule(Rule rule) {
        rules.put(rule, !isRuleEnabled(rule));
    }

    public Map<Rule, Boolean> getRules() {
        return Collections.unmodifiableMap(rules);
    }

    public DuelMode getMode() {
        return mode;
    }

    public void setMode(DuelMode mode) {
        this.mode = mode;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public void setMaxRounds(int maxRounds) {
        if (maxRounds != 1 && maxRounds != 3 && maxRounds != 5) {
            this.maxRounds = 1;
        } else {
            this.maxRounds = maxRounds;
        }
    }

    public int getRoundsToWin() {
        return (maxRounds / 2) + 1;
    }

    public double getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(double betAmount) {
        this.betAmount = Math.max(0.0, betAmount);
    }

    public String getKitName() {
        return kitName;
    }

    public void setKitName(String kitName) {
        this.kitName = kitName;
    }

    public String getArenaName() {
        return arenaName;
    }

    public void setArenaName(String arenaName) {
        this.arenaName = arenaName;
    }

    public RuleSet copy() {
        return new RuleSet(this);
    }
}
