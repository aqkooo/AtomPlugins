package com.ejyqyl.atompvpbot.ai;

import com.ejyqyl.atompvpbot.entity.PvPBotEntity;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Handles attack timing, combos, sprint resets, critical hits, axe shield breaking, and mace smash.
 *
 * @author ejyqyl
 */
public class CombatSystem {

    private final PvPBotEntity bot;
    private final Random random = new Random();

    private long lastAttackTime = 0;
    private int comboCount = 0;
    private long lastComboHitTime = 0;

    public CombatSystem(PvPBotEntity bot) {
        this.bot = bot;
    }

    public void updateCombat() {
        Mob mob = bot.getMob();
        if (mob == null || !mob.isValid()) return;

        Player target = bot.getTargetingSystem().getCurrentTarget();
        if (target == null || !target.isValid()) {
            comboCount = 0;
            return;
        }

        double distance = mob.getLocation().distance(target.getLocation());
        double reach = bot.getEffectiveReach();

        if (distance > reach) {
            // Reset combo if player got away for more than 1.5s
            if (System.currentTimeMillis() - lastComboHitTime > 1500) {
                comboCount = 0;
            }
            return;
        }

        // CPS calculation: interval between clicks = 1000ms / CPS
        int cps = bot.getEffectiveCps();
        long attackInterval = 1000L / Math.max(1, cps);

        // Add slight randomized jitter to interval for human simulation
        attackInterval += (random.nextInt(35) - 17);

        long now = System.currentTimeMillis();
        if (now - lastAttackTime < attackInterval) {
            return;
        }

        lastAttackTime = now;
        executeAttack(target, distance);
    }

    private void executeAttack(Player target, double distance) {
        Mob mob = bot.getMob();

        // Lower shield before striking
        bot.getDefenseSystem().lowerShield();

        // Check if target is blocking with shield -> switch to axe if enabled
        if (target.isBlocking() && bot.canBreakShieldWithAxe()) {
            if (bot.getInventorySystem().switchToAxe()) {
                // Perform shield break
                strike(target, true);
                bot.getInventorySystem().restoreDefaultWeapon();
                return;
            }
        }

        // Check for Mace smash if bot has Mace and is falling
        if (bot.canMaceSmash() && mob.getVelocity().getY() < -0.15) {
            if (bot.getInventorySystem().switchToMace()) {
                strikeMace(target);
                return;
            }
        }

        // Standard melee strike
        strike(target, false);
    }

    private void strike(Player target, boolean isAxeShieldBreak) {
        Mob mob = bot.getMob();
        mob.swingMainHand();

        double baseDamage = calculateBaseDamage();
        boolean isCrit = false;

        // Check if jump-crit condition met (falling downwards)
        if (mob.getVelocity().getY() < -0.05 && !mob.isOnGround()) {
            isCrit = true;
            baseDamage *= 1.5;
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.2, 0), 15, 0.3, 0.3, 0.3, 0.1);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
        }

        // Shield break handling
        if (isAxeShieldBreak && target.isBlocking()) {
            target.setCooldown(Material.SHIELD, 100); // 5 seconds shield disable
            target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.9f);
        }

        // Damage target
        target.damage(baseDamage, mob);

        // Sprint-Reset combo chaining
        handleSprintResetCombo(target);
    }

    private void strikeMace(Player target) {
        Mob mob = bot.getMob();
        mob.swingMainHand();

        // Mace kinetic bonus based on fall distance / velocity
        double bonus = Math.min(12.0, Math.abs(mob.getVelocity().getY()) * 8.0);
        double totalDamage = 7.0 + bonus;

        target.damage(totalDamage, mob);

        target.getWorld().spawnParticle(Particle.DUST_PILLAR, target.getLocation(), 20, 0.5, 0.2, 0.5, 0.1);
        try {
            target.getWorld().playSound(target.getLocation(), Sound.valueOf("ITEM_MACE_SMASH_GROUND"), 1.0f, 1.0f);
        } catch (Throwable ignored) {
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.3f);
        }
    }

    private void handleSprintResetCombo(Player target) {
        Mob mob = bot.getMob();
        long now = System.currentTimeMillis();

        if (now - lastComboHitTime <= 1000) {
            comboCount++;
        } else {
            comboCount = 1;
        }
        lastComboHitTime = now;

        int maxCombo = bot.getEffectiveComboMaxHits();
        if (bot.isSprintResetEnabled() && comboCount <= maxCombo) {
            // Sprint reset impulse: micro-stop followed by forward burst
            Vector dir = target.getLocation().toVector().subtract(mob.getLocation().toVector()).setY(0);
            if (dir.lengthSquared() > 0.01) {
                dir.normalize();
                Vector burst = dir.multiply(0.22);
                mob.setVelocity(new Vector(burst.getX(), mob.getVelocity().getY(), burst.getZ()));
            }
        }
    }

    private double calculateBaseDamage() {
        Mob mob = bot.getMob();
        ItemStack weapon = mob.getEquipment() != null ? mob.getEquipment().getItemInMainHand() : null;
        if (weapon == null || weapon.getType() == Material.AIR) return 2.0;

        Material mat = weapon.getType();
        if (mat == Material.NETHERITE_SWORD) return 8.0;
        if (mat == Material.DIAMOND_SWORD) return 7.0;
        if (mat == Material.IRON_SWORD) return 6.0;
        if (mat == Material.NETHERITE_AXE) return 10.0;
        if (mat == Material.DIAMOND_AXE) return 9.0;
        if (mat == Material.IRON_AXE) return 9.0;
        if (mat == Material.MACE) return 7.0;

        return 5.0;
    }

    public int getComboCount() {
        return comboCount;
    }
}
