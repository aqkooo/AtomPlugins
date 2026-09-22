package com.ejyqyl.atompvpbot.ai;

import com.ejyqyl.atompvpbot.entity.PvPBotEntity;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles potion consumption, golden apple consumption, and survival triage.
 *
 * @author ejyqyl
 */
public class SurvivalSystem {

    private final PvPBotEntity bot;
    private long lastHealTime = 0;

    public SurvivalSystem(PvPBotEntity bot) {
        this.bot = bot;
    }

    public void updateSurvival() {
        Mob mob = bot.getMob();
        if (mob == null || !mob.isValid()) return;

        if (!bot.canUsePotions()) return;

        long now = System.currentTimeMillis();
        if (now - lastHealTime < 5000) return; // 5s cooldown between heals

        double health = mob.getHealth();
        double maxHealth = mob.getMaxHealth();

        // Critical HP (< 40%): Instant Health Potion
        if (health <= maxHealth * 0.40) {
            consumeHealingPotion();
            lastHealTime = now;
            return;
        }

        // Moderate HP (< 60%): Golden Apple / Regeneration
        if (health <= maxHealth * 0.60 && !mob.hasPotionEffect(PotionEffectType.REGENERATION)) {
            consumeGoldenApple();
            lastHealTime = now;
        }
    }

    private void consumeHealingPotion() {
        Mob mob = bot.getMob();
        mob.setHealth(Math.min(mob.getMaxHealth(), mob.getHealth() + 6.0));
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 1.0f, 1.0f);
        mob.getWorld().spawnParticle(Particle.SPLASH, mob.getLocation().add(0, 0.5, 0), 25, 0.3, 0.3, 0.3, 0.05);
    }

    private void consumeGoldenApple() {
        Mob mob = bot.getMob();
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
        mob.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 1200, 0));
        mob.getWorld().spawnParticle(Particle.HEART, mob.getLocation().add(0, 1.2, 0), 6, 0.3, 0.3, 0.3, 0.05);
    }
}
