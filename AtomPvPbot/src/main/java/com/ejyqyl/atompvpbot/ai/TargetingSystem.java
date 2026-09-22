package com.ejyqyl.atompvpbot.ai;

import com.ejyqyl.atompvpbot.entity.PvPBotEntity;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Manages player target selection, line of sight, and boundary tracking.
 *
 * @author ejyqyl
 */
public class TargetingSystem {

    private final PvPBotEntity bot;
    private Player currentTarget;
    private long lastTargetScan = 0;

    public TargetingSystem(PvPBotEntity bot) {
        this.bot = bot;
    }

    public void update() {
        if (bot.getMob() == null || !bot.getMob().isValid()) return;

        // If locked to a fight opponent, maintain that target as long as valid
        if (bot.getFightOpponent() != null) {
            Player opponent = bot.getFightOpponent();
            if (isEligible(opponent)) {
                this.currentTarget = opponent;
                return;
            } else {
                this.currentTarget = null;
                return;
            }
        }

        // Otherwise scan for nearest survival player within 32 blocks
        long now = System.currentTimeMillis();
        if (now - lastTargetScan > 500) {
            lastTargetScan = now;
            scanNearestPlayer();
        }
    }

    private void scanNearestPlayer() {
        if (bot.getMob() == null) return;
        Location botLoc = bot.getMob().getLocation();
        Player closest = null;
        double closestDistSq = 32.0 * 32.0;

        for (Player p : botLoc.getWorld().getPlayers()) {
            if (isEligible(p)) {
                double distSq = p.getLocation().distanceSquared(botLoc);
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closest = p;
                }
            }
        }
        this.currentTarget = closest;
    }

    public boolean isEligible(Player player) {
        if (player == null || !player.isOnline() || !player.isValid()) return false;
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return false;
        if (player.isDead() || player.getHealth() <= 0) return false;
        if (bot.getMob() == null) return false;
        if (!player.getWorld().equals(bot.getMob().getWorld())) return false;

        // Arena boundary check if bot is in an arena
        if (bot.getArena() != null && !bot.getArena().contains(player.getLocation())) {
            return false;
        }

        return true;
    }

    public boolean hasLineOfSight() {
        if (bot.getMob() == null || currentTarget == null) return false;
        return bot.getMob().hasLineOfSight(currentTarget);
    }

    public double getDistance() {
        if (bot.getMob() == null || currentTarget == null) return Double.MAX_VALUE;
        return bot.getMob().getLocation().distance(currentTarget.getLocation());
    }

    public Player getCurrentTarget() {
        return currentTarget;
    }

    public void setCurrentTarget(Player target) {
        this.currentTarget = target;
    }
}
