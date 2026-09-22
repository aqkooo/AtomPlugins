package ru.atomicsqd.atomregen.service;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.atomicsqd.atomregen.AtomRegen;
import ru.atomicsqd.atomregen.model.BlockVector;
import ru.atomicsqd.atomregen.model.CuboidRegion;
import ru.atomicsqd.atomregen.model.PendingRegen;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance ticker that processes scheduled block regenerations without causing TPS drops.
 */
public class RegenEngine {

    private final AtomRegen plugin;
    private final RegionManager regionManager;
    private final Map<String, PendingRegen> pendingByLoc = new ConcurrentHashMap<>();
    private BukkitTask tickerTask;

    public RegenEngine(AtomRegen plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
    }

    public void start() {
        stop();
        this.tickerTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void stop() {
        if (tickerTask != null && !tickerTask.isCancelled()) {
            tickerTask.cancel();
            tickerTask = null;
        }
    }

    private void tick() {
        if (pendingByLoc.isEmpty()) return;

        long now = System.currentTimeMillis();
        int maxPerTick = plugin.getConfig().getInt("performance.max-blocks-per-tick", 25);
        int processed = 0;

        Iterator<Map.Entry<String, PendingRegen>> it = pendingByLoc.entrySet().iterator();
        while (it.hasNext() && processed < maxPerTick) {
            Map.Entry<String, PendingRegen> entry = it.next();
            PendingRegen pending = entry.getValue();

            if (now >= pending.getScheduledTimeMillis()) {
                it.remove();
                restoreBlock(pending);
                processed++;
            }
        }
    }

    private void restoreBlock(PendingRegen pending) {
        World world = Bukkit.getWorld(pending.getWorldName());
        if (world == null) return;

        BlockVector vec = pending.getVector();
        int chunkX = vec.getX() >> 4;
        int chunkZ = vec.getZ() >> 4;

        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            // Keep delayed until chunk loads or defer
            return;
        }

        Block block = world.getBlockAt(vec.getX(), vec.getY(), vec.getZ());
        try {
            BlockData targetData = Bukkit.createBlockData(pending.getTargetBlockDataString());
            block.setBlockData(targetData, false);
            playEffects(world, block.getLocation(), pending.isTemporaryPlaced());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to restore block at " + vec + ": " + e.getMessage());
        }
    }

    public void queueBlock(Location loc, boolean temporaryPlaced) {
        if (loc == null || loc.getWorld() == null) return;

        CuboidRegion region = regionManager.getRegionAt(loc);
        if (region == null) return;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        String snapshotData = region.getSnapshotData(x, y, z);
        if (snapshotData == null) {
            snapshotData = "minecraft:air";
        }

        long delayMs = region.getRegenDelaySeconds() * 1000L;
        long scheduledTime = System.currentTimeMillis() + delayMs;

        String key = loc.getWorld().getName() + ":" + x + "," + y + "," + z;
        BlockVector vec = new BlockVector(x, y, z);

        PendingRegen pending = new PendingRegen(
                loc.getWorld().getName(),
                vec,
                scheduledTime,
                snapshotData,
                temporaryPlaced
        );

        pendingByLoc.put(key, pending);
    }

    public void clearPendingForRegion(CuboidRegion region) {
        if (region == null) return;
        pendingByLoc.entrySet().removeIf(e -> {
            PendingRegen p = e.getValue();
            return region.contains(p.getVector().getX(), p.getVector().getY(), p.getVector().getZ(), p.getWorldName());
        });
    }

    public int getPendingCount() {
        return pendingByLoc.size();
    }

    private void playEffects(World world, Location loc, boolean temporaryPlaced) {
        boolean particlesEnabled = plugin.getConfig().getBoolean("effects.particles.enabled", true);
        boolean soundsEnabled = plugin.getConfig().getBoolean("effects.sounds.enabled", true);

        Location center = loc.clone().add(0.5, 0.5, 0.5);

        if (particlesEnabled) {
            try {
                String typeStr = plugin.getConfig().getString("effects.particles.type", "DUST");
                if ("DUST".equalsIgnoreCase(typeStr)) {
                    String hexColor = plugin.getConfig().getString("effects.particles.dust-color", "#FF8A00");
                    float size = (float) plugin.getConfig().getDouble("effects.particles.dust-size", 1.0);
                    java.awt.Color awtColor = java.awt.Color.decode(hexColor);
                    Color bukkitColor = Color.fromRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
                    Particle.DustOptions dust = new Particle.DustOptions(bukkitColor, size);
                    world.spawnParticle(Particle.DUST, center, 8, 0.25, 0.25, 0.25, dust);
                } else {
                    Particle p = Particle.valueOf(typeStr.toUpperCase());
                    world.spawnParticle(p, center, 8, 0.25, 0.25, 0.25, 0.05);
                }
            } catch (Exception ignored) {}
        }

        if (soundsEnabled) {
            try {
                String soundName = temporaryPlaced
                        ? plugin.getConfig().getString("effects.sounds.vanish-sound", "BLOCK_LAVA_EXTINGUISH")
                        : plugin.getConfig().getString("effects.sounds.restore-sound", "BLOCK_STONE_PLACE");
                float vol = (float) plugin.getConfig().getDouble("effects.sounds.volume", 0.6);
                float pitch = (float) plugin.getConfig().getDouble("effects.sounds.pitch", 1.2);
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                world.playSound(loc, sound, vol, pitch);
            } catch (Exception ignored) {}
        }
    }
}
