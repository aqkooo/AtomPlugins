package com.ejyqyl.atompvpbot.ai;

import com.ejyqyl.atompvpbot.entity.PvPBotEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Handles ranged attacks (bow/crossbow) and tactical ender pearl clutch/recovery.
 *
 * @author ejyqyl
 */
public class UtilitySystem {

    private final PvPBotEntity bot;
    private final Random random = new Random();

    private long lastBowShot = 0;
    private long lastPearlThrow = 0;

    public UtilitySystem(PvPBotEntity bot) {
        this.bot = bot;
    }

    public void updateUtility() {
        Mob mob = bot.getMob();
        if (mob == null || !mob.isValid()) return;

        Player target = bot.getTargetingSystem().getCurrentTarget();
        if (target == null || !target.isValid()) return;

        double distance = mob.getLocation().distance(target.getLocation());
        long now = System.currentTimeMillis();

        // Ender Pearl clutch / gap-closer
        if (bot.canPearlClutch() && now - lastPearlThrow > 15000) {
            // If knocked far (> 14 blocks) or falling down
            if (distance > 14.0 || mob.getLocation().getY() < (bot.getSpawnLocation() != null ? bot.getSpawnLocation().getY() - 4.0 : 0)) {
                throwPearlAt(target.getLocation());
                lastPearlThrow = now;
                return;
            }
        }

        // Ranged combat: bow / crossbow
        if (distance > 8.0 && now - lastBowShot > 2500) {
            if (bot.getInventorySystem().switchToBow()) {
                shootArrowAt(target);
                lastBowShot = now;
                // Switch back to melee after shot
                bot.getInventorySystem().restoreDefaultWeapon();
            }
        }
    }

    private void shootArrowAt(Player target) {
        Mob mob = bot.getMob();
        mob.swingMainHand();

        Vector dir = target.getEyeLocation().toVector().subtract(mob.getEyeLocation().toVector());
        // Gravity compensation arc
        dir.add(new Vector(0, dir.length() * 0.05, 0));
        dir.normalize().multiply(2.2);

        Arrow arrow = mob.launchProjectile(Arrow.class, dir);
        arrow.setShooter(mob);
        arrow.setCritical(true);
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
    }

    private void throwPearlAt(Location targetLoc) {
        Mob mob = bot.getMob();
        Vector dir = targetLoc.toVector().subtract(mob.getEyeLocation().toVector());
        dir.setY(dir.getY() + 2.5); // Lob arc
        dir.normalize().multiply(1.5);

        EnderPearl pearl = mob.launchProjectile(EnderPearl.class, dir);
        pearl.setShooter(mob);
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 1.0f);
    }
}
