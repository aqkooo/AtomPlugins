package com.ejyqyl.atompvpbot.ai;

import com.ejyqyl.atompvpbot.entity.PvPBotEntity;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Smooth, human-like rotation and aim interpolation without instant snaps.
 *
 * @author ejyqyl
 */
public class RotationSystem {

    private final PvPBotEntity bot;
    private final Random random = new Random();

    public RotationSystem(PvPBotEntity bot) {
        this.bot = bot;
    }

    public void updateRotation() {
        Mob mob = bot.getMob();
        if (mob == null || !mob.isValid()) return;

        Player target = bot.getTargetingSystem().getCurrentTarget();
        if (target == null || !target.isValid()) return;

        Location eyeLoc = mob.getEyeLocation();
        Location targetEye = target.getEyeLocation();

        Vector dir = targetEye.toVector().subtract(eyeLoc.toVector());
        if (dir.lengthSquared() < 0.0001) return;

        // Calculate target Yaw and Pitch
        double dx = dir.getX();
        double dy = dir.getY();
        double dz = dir.getZ();
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) Math.toDegrees(-Math.atan2(dy, distanceXZ));

        // Add small inaccuracy noise
        double accuracy = bot.getEffectiveAimAccuracy();
        if (accuracy < 0.99) {
            float noiseFactor = (float) ((1.0 - accuracy) * 12.0);
            targetYaw += (random.nextFloat() - 0.5f) * noiseFactor;
            targetPitch += (random.nextFloat() - 0.5f) * (noiseFactor * 0.6f);
        }

        float currentYaw = mob.getLocation().getYaw();
        float currentPitch = mob.getLocation().getPitch();

        // Wrap yaw difference to [-180, 180]
        float yawDiff = wrapAngle(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        float maxSpeed = bot.getEffectiveAimSpeed();

        // Clamp rotation delta
        float stepYaw = Math.clamp(yawDiff, -maxSpeed, maxSpeed);
        float stepPitch = Math.clamp(pitchDiff, -maxSpeed * 0.8f, maxSpeed * 0.8f);

        float newYaw = currentYaw + stepYaw;
        float newPitch = Math.clamp(currentPitch + stepPitch, -90.0f, 90.0f);

        mob.setRotation(newYaw, newPitch);
    }

    private float wrapAngle(float angle) {
        float wrapped = angle % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        }
        if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }
}
