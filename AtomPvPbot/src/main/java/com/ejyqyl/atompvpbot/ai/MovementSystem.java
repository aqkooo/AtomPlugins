package com.ejyqyl.atompvpbot.ai;

import com.ejyqyl.atompvpbot.entity.PvPBotEntity;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Handles navigation, strafing, jump-crits, retreat, and unstuck logic.
 *
 * @author ejyqyl
 */
public class MovementSystem {

    private final PvPBotEntity bot;
    private final Random random = new Random();

    private int strafeDirection = 1; // 1 = right, -1 = left
    private int strafeTicksRemaining = 0;
    private long lastJumpCrit = 0;

    // Stuck detector
    private Location lastCheckLocation;
    private int stuckTicks = 0;

    public MovementSystem(PvPBotEntity bot) {
        this.bot = bot;
    }

    public void updateMovement() {
        Mob mob = bot.getMob();
        if (mob == null || !mob.isValid()) return;

        Player target = bot.getTargetingSystem().getCurrentTarget();
        if (target == null || !target.isValid()) {
            // Idle or return to spawn
            handleIdle();
            return;
        }

        checkStuck(mob.getLocation(), target.getLocation());

        double dist = mob.getLocation().distance(target.getLocation());
        BotBehaviorMode mode = bot.getBehaviorMode();
        double speed = 1.05 * mode.getSpeedMultiplier();

        // Retreat condition for defensive/balanced modes
        if (mode.isRetreatOnLowHp() && mob.getHealth() < 8.0) {
            retreatFrom(target.getLocation(), speed);
            return;
        }

        // Close range combat movement: strafe & jump-crit
        if (dist <= 4.0) {
            handleCombatMovement(target, dist, speed);
        } else {
            // Rush target via Paper pathfinder
            try {
                mob.getPathfinder().moveTo(target, speed);
            } catch (Throwable ignored) {
                // Fallback direct velocity if pathfinder unavailable
                Vector dir = target.getLocation().toVector().subtract(mob.getLocation().toVector()).setY(0).normalize();
                mob.setVelocity(mob.getVelocity().add(dir.multiply(0.12)));
            }
        }
    }

    private void handleCombatMovement(Player target, double dist, double baseSpeed) {
        Mob mob = bot.getMob();

        // Update strafe direction timer
        if (--strafeTicksRemaining <= 0) {
            strafeDirection = random.nextBoolean() ? 1 : -1;
            strafeTicksRemaining = 15 + random.nextInt(25); // Switch direction every 0.7 - 2 seconds
        }

        Vector toTarget = target.getLocation().toVector().subtract(mob.getLocation().toVector()).setY(0);
        if (toTarget.lengthSquared() > 0.001) {
            toTarget.normalize();
        }

        // Orthogonal strafe vector (perpendicular)
        Vector strafeVec = new Vector(-toTarget.getZ(), 0, toTarget.getX()).multiply(strafeDirection);

        // Blend forward approach with sideways strafe
        Vector moveGoal;
        if (dist < 2.0) {
            // Too close: back up slightly or circle
            moveGoal = strafeVec.clone().multiply(1.5).subtract(toTarget.clone().multiply(0.5));
        } else if (dist > 3.2) {
            // Close the gap while angling in
            moveGoal = toTarget.clone().multiply(1.2).add(strafeVec.clone().multiply(0.8));
        } else {
            // Sweet spot: pure strafe circle
            moveGoal = strafeVec.clone().multiply(1.5).add(toTarget.clone().multiply(0.3));
        }

        Location targetDest = mob.getLocation().add(moveGoal.multiply(2.0));
        try {
            mob.getPathfinder().moveTo(targetDest, baseSpeed);
        } catch (Throwable ignored) {}

        // Small physics impulse for realistic strafing
        Vector currentVel = mob.getVelocity();
        Vector horizontalImpulse = moveGoal.clone().normalize().multiply(0.08);
        mob.setVelocity(new Vector(currentVel.getX() + horizontalImpulse.getX(), currentVel.getY(), currentVel.getZ() + horizontalImpulse.getZ()));

        // Jump-Crit trigger
        tryJumpCrit(dist);
    }

    private void tryJumpCrit(double dist) {
        Mob mob = bot.getMob();
        if (dist > 3.6 || dist < 1.5) return;
        if (!mob.isOnGround()) return;

        long now = System.currentTimeMillis();
        if (now - lastJumpCrit < 900) return; // Cooldown between jumps

        double critChance = bot.getEffectiveJumpCritChance();
        if (random.nextDouble() <= critChance) {
            lastJumpCrit = now;
            // Minecraft player jump velocity is ~0.42
            Vector v = mob.getVelocity();
            mob.setVelocity(new Vector(v.getX() * 1.15, 0.38, v.getZ() * 1.15));
        }
    }

    private void retreatFrom(Location danger, double speed) {
        Mob mob = bot.getMob();
        Vector away = mob.getLocation().toVector().subtract(danger.toVector()).setY(0);
        if (away.lengthSquared() > 0.001) {
            away.normalize();
        }
        Location dest = mob.getLocation().add(away.multiply(5.0));
        try {
            mob.getPathfinder().moveTo(dest, speed * 1.1);
        } catch (Throwable ignored) {}
    }

    private void handleIdle() {
        Mob mob = bot.getMob();
        if (bot.getSpawnLocation() != null && mob.getLocation().distanceSquared(bot.getSpawnLocation()) > 4.0) {
            try {
                mob.getPathfinder().moveTo(bot.getSpawnLocation(), 1.0);
            } catch (Throwable ignored) {}
        }
    }

    private void checkStuck(Location currentLoc, Location targetLoc) {
        Mob mob = bot.getMob();
        if (lastCheckLocation == null) {
            lastCheckLocation = currentLoc.clone();
            return;
        }

        if (currentLoc.distanceSquared(lastCheckLocation) < 0.08) {
            stuckTicks++;
            if (stuckTicks > 25) { // Stuck for > 1.25s
                // Jump over obstacle
                if (mob.isOnGround()) {
                    mob.setVelocity(new Vector(mob.getVelocity().getX(), 0.42, mob.getVelocity().getZ()));
                }
            }
            if (stuckTicks > 60) { // Stuck for > 3s: nudge forward
                Vector dir = targetLoc.toVector().subtract(currentLoc.toVector()).setY(0.2);
                if (dir.lengthSquared() > 0.01) dir.normalize().multiply(0.7);
                mob.setVelocity(dir);
                stuckTicks = 0;
            }
        } else {
            stuckTicks = 0;
            lastCheckLocation = currentLoc.clone();
        }
    }
}
