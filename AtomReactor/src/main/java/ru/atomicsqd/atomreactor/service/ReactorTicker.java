package ru.atomicsqd.atomreactor.service;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.atomicsqd.atomreactor.AtomReactor;
import ru.atomicsqd.atomreactor.config.ReactorLevel;
import ru.atomicsqd.atomreactor.economy.EconomyService;
import ru.atomicsqd.atomreactor.model.GenerationTarget;
import ru.atomicsqd.atomreactor.model.Reactor;
import ru.atomicsqd.atomreactor.util.ColorUtil;

/**
 * Global master ticker executing income generation, particle effects, and hologram updates.
 */
public class ReactorTicker {

    private final AtomReactor plugin;
    private final ReactorManager reactorManager;
    private final HologramService hologramService;
    private final EconomyService economyService;

    private BukkitTask tickerTask;
    private double particleAngle = 0.0;

    public ReactorTicker(AtomReactor plugin, ReactorManager reactorManager,
                         HologramService hologramService, EconomyService economyService) {
        this.plugin = plugin;
        this.reactorManager = reactorManager;
        this.hologramService = hologramService;
        this.economyService = economyService;
    }

    public void start() {
        stop();
        this.tickerTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 20L, 20L); // Every 1 second
    }

    public void stop() {
        if (tickerTask != null && !tickerTask.isCancelled()) {
            tickerTask.cancel();
            tickerTask = null;
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        particleAngle += Math.PI / 8;
        if (particleAngle > 2 * Math.PI) {
            particleAngle = 0.0;
        }

        for (Reactor reactor : reactorManager.getAllReactors()) {
            Location loc = reactor.getLocation();
            if (loc == null || loc.getWorld() == null) continue;

            int chunkX = loc.getBlockX() >> 4;
            int chunkZ = loc.getBlockZ() >> 4;
            if (!loc.getWorld().isChunkLoaded(chunkX, chunkZ)) continue;

            ReactorLevel level = plugin.getConfigManager().getLevel(reactor.getLevel());
            if (level == null) continue;

            // 1. Particle effect around reactor
            spawnReactorParticles(loc, level);

            // 2. Check if active
            if (!reactor.isActive()) {
                hologramService.updateHologram(reactor);
                continue;
            }

            // 3. Check owner online status
            Player owner = Bukkit.getPlayer(reactor.getOwnerUuid());
            boolean ownerOnline = owner != null && owner.isOnline();

            if (plugin.getConfigManager().isOwnerMustBeOnline() && !ownerOnline) {
                hologramService.updateHologram(reactor);
                continue;
            }

            // 4. Check generation interval
            long elapsedSeconds = (now - reactor.getLastGenerationTime()) / 1000L;
            if (elapsedSeconds >= level.getIntervalSeconds()) {
                reactor.setLastGenerationTime(now);
                processIncome(reactor, level, owner);
                hologramService.updateHologram(reactor);
            }
        }
    }

    private void processIncome(Reactor reactor, ReactorLevel level, Player owner) {
        GenerationTarget target = plugin.getConfigManager().getGenerationTarget();
        Location rLoc = reactor.getLocation();
        double income = level.getIncome();
        int radius = level.getRadius();

        switch (target) {
            case OWNER_GLOBAL -> {
                if (owner != null && owner.isOnline()) {
                    economyService.deposit(owner, income);
                    sendIncomeMessage(owner, income, level);
                }
            }
            case OWNER_IN_RADIUS -> {
                if (owner != null && owner.isOnline()) {
                    if (owner.getWorld().equals(rLoc.getWorld()) && owner.getLocation().distance(rLoc) <= radius) {
                        economyService.deposit(owner, income);
                        sendIncomeMessage(owner, income, level);
                    }
                }
            }
            case ALL_IN_RADIUS -> {
                World world = rLoc.getWorld();
                if (world != null) {
                    for (Player p : world.getPlayers()) {
                        if (p.getLocation().distance(rLoc) <= radius) {
                            economyService.deposit(p, income);
                            sendIncomeMessage(p, income, level);
                        }
                    }
                }
            }
            case INTERNAL_VAULT -> {
                reactor.depositStored(income, level.getMaxStorage());
            }
        }
    }

    private void sendIncomeMessage(Player player, double amount, ReactorLevel level) {
        String raw = plugin.getConfigManager().getMessage("income-received")
                .replace("%amount%", String.format("%.1f", amount))
                .replace("%currency%", plugin.getConfigManager().getCurrencySymbol())
                .replace("%level_name%", level.getName())
                .replace("%level%", String.valueOf(level.getLevel()));

        player.sendActionBar(ColorUtil.component(raw));
    }

    private void spawnReactorParticles(Location loc, ReactorLevel level) {
        World world = loc.getWorld();
        if (world == null) return;

        double centerX = loc.getBlockX() + 0.5;
        double centerY = loc.getBlockY() + 0.5;
        double centerZ = loc.getBlockZ() + 0.5;

        String pType = level.getParticle();
        if ("DUST".equalsIgnoreCase(pType)) {
            try {
                java.awt.Color awt = java.awt.Color.decode(level.getParticleColor());
                Color bukkitColor = Color.fromRGB(awt.getRed(), awt.getGreen(), awt.getBlue());
                Particle.DustOptions dust = new Particle.DustOptions(bukkitColor, 1.2f);

                double px = centerX + 0.7 * Math.cos(particleAngle);
                double pz = centerZ + 0.7 * Math.sin(particleAngle);
                world.spawnParticle(Particle.DUST, px, centerY, pz, 1, dust);

                double px2 = centerX + 0.7 * Math.cos(particleAngle + Math.PI);
                double pz2 = centerZ + 0.7 * Math.sin(particleAngle + Math.PI);
                world.spawnParticle(Particle.DUST, px2, centerY, pz2, 1, dust);
            } catch (Exception ignored) {}
        } else {
            try {
                Particle p = Particle.valueOf(pType.toUpperCase());
                world.spawnParticle(p, centerX, centerY + 0.6, centerZ, 2, 0.1, 0.1, 0.1, 0.02);
            } catch (Exception ignored) {}
        }
    }
}
