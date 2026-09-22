package com.ejyqyl.atompvpbot.gui;

import com.ejyqyl.atompvpbot.ai.BotBehaviorMode;
import com.ejyqyl.atompvpbot.ai.BotDifficulty;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores transient player lobby choices (kit, difficulty, behavior, arena).
 *
 * @author ejyqyl
 */
public class PlayerFightPreferences {

    private static final Map<UUID, PlayerFightPreferences> PREFERENCES = new ConcurrentHashMap<>();

    private BotDifficulty difficulty = BotDifficulty.NORMAL;
    private BotBehaviorMode behaviorMode = BotBehaviorMode.BALANCED;
    private String kitName = "classic";
    private String arenaName = null; // null = any free arena
    private boolean continuousTraining = true;

    public static PlayerFightPreferences get(UUID uuid) {
        return PREFERENCES.computeIfAbsent(uuid, k -> new PlayerFightPreferences());
    }

    public BotDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(BotDifficulty difficulty) { this.difficulty = difficulty; }

    public BotBehaviorMode getBehaviorMode() { return behaviorMode; }
    public void setBehaviorMode(BotBehaviorMode behaviorMode) { this.behaviorMode = behaviorMode; }

    public String getKitName() { return kitName; }
    public void setKitName(String kitName) { this.kitName = kitName; }

    public String getArenaName() { return arenaName; }
    public void setArenaName(String arenaName) { this.arenaName = arenaName; }

    public boolean isContinuousTraining() { return continuousTraining; }
    public void setContinuousTraining(boolean continuousTraining) { this.continuousTraining = continuousTraining; }
}
