package com.ejyqyl.glowchatgame.config;

import com.ejyqyl.glowchatgame.GlowChatGame;
import com.ejyqyl.glowchatgame.game.Difficulty;
import com.ejyqyl.glowchatgame.game.MathOperation;
import com.ejyqyl.glowchatgame.reward.RewardGroup;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Manages configuration loading, parsing, and caching for GlowChatGame.
 *
 * @author ejyqyl, Glowdevv
 */
public final class ConfigManager {

    private final GlowChatGame plugin;

    private boolean gameEnabled;
    private int interval;
    private int answerTime;
    private int minPlayers;
    private String defaultDifficulty;

    private boolean soundsEnabled;
    private String soundStart;
    private String soundWin;
    private String soundTimeout;

    private boolean additionEnabled;
    private boolean subtractionEnabled;
    private boolean multiplicationEnabled;
    private boolean divisionEnabled;
    private boolean divisionIntegerOnly;

    private final Map<Difficulty, Integer> minNumbers = new EnumMap<>(Difficulty.class);
    private final Map<Difficulty, Integer> maxNumbers = new EnumMap<>(Difficulty.class);
    private final Map<Difficulty, Boolean> multiSteps = new EnumMap<>(Difficulty.class);
    private final Map<Difficulty, Integer> multiStepChances = new EnumMap<>(Difficulty.class);
    private final Map<Difficulty, List<MathOperation>> difficultyOperations = new EnumMap<>(Difficulty.class);

    private boolean rewardsEnabled;
    private final List<RewardGroup> rewardGroups = new ArrayList<>();

    private boolean statsEnabled;
    private String databaseFile;

    public ConfigManager(@NotNull GlowChatGame plugin) {
        this.plugin = plugin;
        reload();
    }

    public synchronized void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Game settings
        this.gameEnabled = config.getBoolean("game.enabled", true);
        this.interval = Math.max(10, config.getInt("game.interval", 300));
        this.answerTime = Math.max(5, config.getInt("game.answer-time", 30));
        this.minPlayers = Math.max(0, config.getInt("game.min-players", 1));
        this.defaultDifficulty = config.getString("game.default-difficulty", "random").toLowerCase(Locale.ROOT);

        // Sounds
        this.soundsEnabled = config.getBoolean("game.sounds.enabled", true);
        this.soundStart = config.getString("game.sounds.start", "ENTITY_PLAYER_LEVELUP");
        this.soundWin = config.getString("game.sounds.win", "UI_TOAST_CHALLENGE_COMPLETE");
        this.soundTimeout = config.getString("game.sounds.timeout", "BLOCK_NOTE_BLOCK_BASS");

        // Global operations
        this.additionEnabled = config.getBoolean("operations.addition", true);
        this.subtractionEnabled = config.getBoolean("operations.subtraction", true);
        this.multiplicationEnabled = config.getBoolean("operations.multiplication", true);
        this.divisionEnabled = config.getBoolean("operations.division", true);
        this.divisionIntegerOnly = config.getBoolean("operations.division-integer-only", true);

        // Difficulties
        loadDifficulty(Difficulty.EASY, config.getConfigurationSection("difficulty.easy"), 1, 20, false, 0);
        loadDifficulty(Difficulty.MEDIUM, config.getConfigurationSection("difficulty.medium"), 1, 100, false, 30);
        loadDifficulty(Difficulty.HARD, config.getConfigurationSection("difficulty.hard"), 10, 500, true, 60);

        // Rewards
        this.rewardsEnabled = config.getBoolean("rewards.enabled", true);
        this.rewardGroups.clear();
        ConfigurationSection rewardsSec = config.getConfigurationSection("rewards.list");
        if (rewardsSec != null) {
            for (String key : rewardsSec.getKeys(false)) {
                ConfigurationSection tier = rewardsSec.getConfigurationSection(key);
                if (tier != null) {
                    int chance = tier.getInt("chance", 100);
                    String name = tier.getString("display-name", "$500");
                    List<String> commands = tier.getStringList("commands");
                    rewardGroups.add(new RewardGroup(chance, name, commands));
                }
            }
        }
        if (rewardGroups.isEmpty()) {
            rewardGroups.add(new RewardGroup(100, "$500", List.of("eco give %player% 500")));
        }

        // Stats
        this.statsEnabled = config.getBoolean("stats.enabled", true);
        this.databaseFile = config.getString("stats.database.file", "data/stats.db");
    }

    private void loadDifficulty(Difficulty diff, ConfigurationSection sec, int defMin, int defMax, boolean defMulti, int defChance) {
        if (sec == null) {
            minNumbers.put(diff, defMin);
            maxNumbers.put(diff, defMax);
            multiSteps.put(diff, defMulti);
            multiStepChances.put(diff, defChance);
            difficultyOperations.put(diff, Arrays.asList(MathOperation.values()));
            return;
        }

        minNumbers.put(diff, sec.getInt("min-number", defMin));
        maxNumbers.put(diff, sec.getInt("max-number", defMax));
        multiSteps.put(diff, sec.getBoolean("multi-step", defMulti));
        multiStepChances.put(diff, sec.getInt("multi-step-chance", defChance));

        List<String> opStrings = sec.getStringList("operations");
        List<MathOperation> ops = new ArrayList<>();
        if (!opStrings.isEmpty()) {
            for (String opStr : opStrings) {
                ops.add(MathOperation.fromString(opStr));
            }
        } else {
            ops.addAll(Arrays.asList(MathOperation.values()));
        }
        difficultyOperations.put(diff, ops);
    }

    public boolean isGameEnabled() {
        return gameEnabled;
    }

    public int getInterval() {
        return interval;
    }

    public int getAnswerTime() {
        return answerTime;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    @NotNull
    public String getDefaultDifficulty() {
        return defaultDifficulty;
    }

    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    public String getSoundStart() {
        return soundStart;
    }

    public String getSoundWin() {
        return soundWin;
    }

    public String getSoundTimeout() {
        return soundTimeout;
    }

    public boolean isOperationGloballyEnabled(@NotNull MathOperation op) {
        return switch (op) {
            case ADD -> additionEnabled;
            case SUBTRACT -> subtractionEnabled;
            case MULTIPLY -> multiplicationEnabled;
            case DIVIDE -> divisionEnabled;
        };
    }

    public boolean isDivisionIntegerOnly() {
        return divisionIntegerOnly;
    }

    public int getMinNumber(@NotNull Difficulty diff) {
        return minNumbers.getOrDefault(diff, 1);
    }

    public int getMaxNumber(@NotNull Difficulty diff) {
        return maxNumbers.getOrDefault(diff, 100);
    }

    public boolean isMultiStepAllowed(@NotNull Difficulty diff) {
        return multiSteps.getOrDefault(diff, false);
    }

    public int getMultiStepChance(@NotNull Difficulty diff) {
        return multiStepChances.getOrDefault(diff, 0);
    }

    @NotNull
    public List<MathOperation> getOperationsForDifficulty(@NotNull Difficulty diff) {
        return difficultyOperations.getOrDefault(diff, List.of(MathOperation.ADD));
    }

    public boolean isRewardsEnabled() {
        return rewardsEnabled;
    }

    @NotNull
    public List<RewardGroup> getRewardGroups() {
        return Collections.unmodifiableList(rewardGroups);
    }

    public boolean isStatsEnabled() {
        return statsEnabled;
    }

    @NotNull
    public String getDatabaseFile() {
        return databaseFile;
    }
}
