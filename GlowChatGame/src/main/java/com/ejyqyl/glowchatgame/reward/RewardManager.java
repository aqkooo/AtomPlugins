package com.ejyqyl.glowchatgame.reward;

import com.ejyqyl.glowchatgame.GlowChatGame;
import com.ejyqyl.glowchatgame.config.ConfigManager;
import com.ejyqyl.glowchatgame.game.MathProblem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages reward selection, chance rolling, and command dispatching.
 *
 * @author ejyqyl, Glowdevv
 */
public final class RewardManager {

    private final GlowChatGame plugin;
    private final ConfigManager configManager;

    public RewardManager(@NotNull GlowChatGame plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Nullable
    public RewardGroup selectReward() {
        if (!configManager.isRewardsEnabled()) {
            return null;
        }

        List<RewardGroup> groups = configManager.getRewardGroups();
        if (groups.isEmpty()) {
            return null;
        }

        int roll = ThreadLocalRandom.current().nextInt(100) + 1; // 1 to 100
        int cumulative = 0;

        for (RewardGroup group : groups) {
            cumulative += group.getChance();
            if (roll <= cumulative) {
                return group;
            }
        }

        // Fallback to the first configured group
        return groups.get(0);
    }

    public void giveReward(@NotNull Player player, @NotNull MathProblem problem, @Nullable RewardGroup rewardGroup) {
        if (rewardGroup == null) {
            return;
        }

        for (String commandTemplate : rewardGroup.getCommands()) {
            String command = commandTemplate
                    .replace("%player%", player.getName())
                    .replace("%answer%", String.valueOf(problem.getAnswer()))
                    .replace("%difficulty%", problem.getDifficulty().getDisplayName());

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }
}
