package com.ejyqyl.glowchatgame.game;

import com.ejyqyl.glowchatgame.GlowChatGame;
import com.ejyqyl.glowchatgame.api.event.ChatGameStartEvent;
import com.ejyqyl.glowchatgame.api.event.ChatGameTimeoutEvent;
import com.ejyqyl.glowchatgame.api.event.ChatGameWinEvent;
import com.ejyqyl.glowchatgame.config.ConfigManager;
import com.ejyqyl.glowchatgame.config.MessageManager;
import com.ejyqyl.glowchatgame.reward.RewardGroup;
import com.ejyqyl.glowchatgame.reward.RewardManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Main state machine and coordinator for GlowChatGame.
 * Controls recurring intervals, problem broadcasting, timeout triggers, and victory resolution.
 *
 * @author ejyqyl, Glowdevv
 */
public final class GameManager {

    private final GlowChatGame plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final RewardManager rewardManager;
    private final MathProblemGenerator problemGenerator;

    private ActiveGame activeGame;
    private BukkitTask scheduleTask;

    private String lastWinner = "Никто";
    private String lastAnswer = "0";

    public GameManager(@NotNull GlowChatGame plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.rewardManager = new RewardManager(plugin);
        this.problemGenerator = new MathProblemGenerator(configManager);

        startScheduler();
    }

    public void startScheduler() {
        cancelScheduler();

        if (!configManager.isGameEnabled()) {
            return;
        }

        long intervalTicks = configManager.getInterval() * 20L;
        this.scheduleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isGameRunning() && Bukkit.getOnlinePlayers().size() >= configManager.getMinPlayers()) {
                startNewGame(null);
            }
        }, intervalTicks, intervalTicks);
    }

    public void cancelScheduler() {
        if (scheduleTask != null) {
            scheduleTask.cancel();
            this.scheduleTask = null;
        }
    }

    public synchronized boolean startNewGame(@Nullable Difficulty forcedDifficulty) {
        if (isGameRunning()) {
            return false;
        }

        Difficulty difficulty = forcedDifficulty;
        if (difficulty == null) {
            String def = configManager.getDefaultDifficulty();
            if ("random".equalsIgnoreCase(def)) {
                Difficulty[] values = Difficulty.values();
                difficulty = values[ThreadLocalRandom.current().nextInt(values.length)];
            } else {
                difficulty = Difficulty.fromString(def, Difficulty.MEDIUM);
            }
        }

        MathProblem problem = problemGenerator.generate(difficulty);
        RewardGroup reward = rewardManager.selectReward();

        ChatGameStartEvent startEvent = new ChatGameStartEvent(problem);
        Bukkit.getPluginManager().callEvent(startEvent);
        if (startEvent.isCancelled()) {
            return false;
        }

        ActiveGame game = new ActiveGame(problem, reward, configManager.getAnswerTime());
        this.activeGame = game;

        // Schedule timeout task
        long timeoutTicks = configManager.getAnswerTime() * 20L;
        BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> onTimeout(game), timeoutTicks);
        game.setTimeoutTask(timeoutTask);

        // Broadcast announcement
        String rewardName = reward != null ? reward.getDisplayName() : "Слава";
        Map<String, String> placeholders = Map.of(
                "{EXAMPLE}", problem.getExpression(),
                "{REWARD}", rewardName,
                "{TIME}", String.valueOf(configManager.getAnswerTime()),
                "{DIFFICULTY}", difficulty.getDisplayName()
        );
        messageManager.broadcast("game.start", placeholders);

        // Sound
        playSoundServer(configManager.getSoundStart());
        return true;
    }

    public synchronized boolean stopCurrentGame() {
        if (activeGame == null) {
            return false;
        }

        activeGame.forceStop();
        this.activeGame = null;
        messageManager.broadcast("game.stopped", Map.of());
        return true;
    }

    public void processChatAnswer(@NotNull Player player, @NotNull String message) {
        ActiveGame currentGame = this.activeGame;
        if (currentGame == null || currentGame.isSolved()) {
            return;
        }

        if (currentGame.trySolve(player, message)) {
            // Must handle rewards and events on main thread
            Bukkit.getScheduler().runTask(plugin, () -> onWin(player, currentGame));
        }
    }

    private void onWin(@NotNull Player player, @NotNull ActiveGame game) {
        this.lastWinner = player.getName();
        this.lastAnswer = String.valueOf(game.getProblem().getAnswer());
        this.activeGame = null;

        ChatGameWinEvent winEvent = new ChatGameWinEvent(
                player,
                game.getProblem(),
                game.getRewardGroup(),
                game.getTimeTakenSeconds()
        );
        Bukkit.getPluginManager().callEvent(winEvent);

        // Distribute reward
        rewardManager.giveReward(player, game.getProblem(), game.getRewardGroup());

        // Update statistics
        plugin.getStatsManager().recordWin(player);

        // Broadcast winner announcement
        String rewardName = game.getRewardGroup() != null ? game.getRewardGroup().getDisplayName() : "Слава";
        Map<String, String> placeholders = Map.of(
                "{PLAYER}", player.getName(),
                "{EXAMPLE}", game.getProblem().getExpression(),
                "{ANSWER}", String.valueOf(game.getProblem().getAnswer()),
                "{TIME_TAKEN}", String.format(Locale.ROOT, "%.1f", game.getTimeTakenSeconds()),
                "{REWARD}", rewardName,
                "{DIFFICULTY}", game.getProblem().getDifficulty().getDisplayName()
        );
        messageManager.broadcast("game.correct-answer", placeholders);

        // Sound
        playSoundServer(configManager.getSoundWin());
    }

    private void onTimeout(@NotNull ActiveGame game) {
        if (this.activeGame != game) {
            return;
        }

        if (game.forceStop()) {
            this.lastAnswer = String.valueOf(game.getProblem().getAnswer());
            this.activeGame = null;

            ChatGameTimeoutEvent timeoutEvent = new ChatGameTimeoutEvent(game.getProblem());
            Bukkit.getPluginManager().callEvent(timeoutEvent);

            Map<String, String> placeholders = Map.of(
                    "{EXAMPLE}", game.getProblem().getExpression(),
                    "{ANSWER}", String.valueOf(game.getProblem().getAnswer()),
                    "{DIFFICULTY}", game.getProblem().getDifficulty().getDisplayName()
            );
            messageManager.broadcast("game.timeout", placeholders);

            // Sound
            playSoundServer(configManager.getSoundTimeout());
        }
    }

    private void playSoundServer(@Nullable String soundName) {
        if (!configManager.isSoundsEnabled() || soundName == null || soundName.trim().isEmpty()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
        } catch (IllegalArgumentException ignored) {
            // Sound doesn't exist on this MC version, suppress safely
        }
    }

    public boolean isGameRunning() {
        return activeGame != null && !activeGame.isSolved();
    }

    @Nullable
    public ActiveGame getActiveGame() {
        return activeGame;
    }

    @NotNull
    public String getLastWinner() {
        return lastWinner;
    }

    @NotNull
    public String getLastAnswer() {
        return lastAnswer;
    }

    public void reload() {
        cancelScheduler();
        startScheduler();
    }
}
