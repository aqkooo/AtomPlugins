package com.ejyqyl.atompvpbot.ai;

import com.ejyqyl.atompvpbot.entity.PvPBotEntity;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * Handles shield blocking mechanics, axe stun cooldown, and infinite totem resurrection.
 *
 * @author ejyqyl
 */
public class DefenseSystem {

    private final PvPBotEntity bot;
    private final Random random = new Random();

    private boolean isBlocking = false;
    private long shieldCooldownUntil = 0;
    private long lastBlockToggle = 0;

    public DefenseSystem(PvPBotEntity bot) {
        this.bot = bot;
    }

    public void updateDefense() {
        Mob mob = bot.getMob();
        if (mob == null || !mob.isValid()) return;

        Player target = bot.getTargetingSystem().getCurrentTarget();
        if (target == null || !target.isValid()) {
            if (isBlocking) lowerShield();
            return;
        }

        // Shield cooldown check
        long now = System.currentTimeMillis();
        if (now < shieldCooldownUntil) {
            if (isBlocking) lowerShield();
            return;
        }

        // Check if bot has shield in off-hand or main hand
        ItemStack offhand = mob.getEquipment() != null ? mob.getEquipment().getItemInOffHand() : null;
        if (offhand == null || offhand.getType() != Material.SHIELD) {
            if (isBlocking) lowerShield();
            return;
        }

        // Check distance to opponent
        double dist = mob.getLocation().distance(target.getLocation());
        if (dist > 3.8) {
            if (isBlocking) lowerShield();
            return;
        }

        // Tactical blocking decision based on difficulty block chance
        if (now - lastBlockToggle > 300) {
            lastBlockToggle = now;
            double blockChance = bot.getEffectiveShieldBlockChance();
            if (random.nextDouble() <= blockChance) {
                raiseShield();
            } else {
                lowerShield();
            }
        }
    }

    public void raiseShield() {
        if (isBlocking) return;
        Mob mob = bot.getMob();
        if (mob == null || !mob.isValid()) return;

        long now = System.currentTimeMillis();
        if (now < shieldCooldownUntil) return;

        try {
            mob.startUsingItem(EquipmentSlot.OFF_HAND);
            isBlocking = true;
        } catch (Throwable ignored) {
            isBlocking = false;
        }
    }

    public void lowerShield() {
        if (!isBlocking) return;
        Mob mob = bot.getMob();
        if (mob != null && mob.isValid()) {
            try {
                mob.completeUsingActiveItem();
            } catch (Throwable ignored) {}
        }
        isBlocking = false;
    }

    public void breakShield() {
        lowerShield();
        shieldCooldownUntil = System.currentTimeMillis() + 5000; // 5 seconds stun
        Mob mob = bot.getMob();
        if (mob != null && mob.isValid()) {
            mob.getWorld().playSound(mob.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);
        }
    }

    /**
     * Executes infinite totem resurrection effect and replenishes offhand totem.
     */
    public void triggerTotem() {
        Mob mob = bot.getMob();
        if (mob == null || !mob.isValid()) return;

        // Visual and auditory totem effect
        mob.getWorld().playSound(mob.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        mob.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, mob.getLocation().add(0, 1, 0), 40, 0.4, 0.5, 0.4, 0.1);

        // Restore health and grant standard totem buffs
        mob.setHealth(Math.min(mob.getMaxHealth(), 10.0));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));

        // Refill infinite totem in offhand
        if (mob.getEquipment() != null) {
            mob.getEquipment().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
        }
    }

    public boolean isBlocking() {
        return isBlocking;
    }

    public boolean isShieldOnCooldown() {
        return System.currentTimeMillis() < shieldCooldownUntil;
    }
}
