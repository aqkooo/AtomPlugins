package com.ejyqyl.atombot.entity;

import com.ejyqyl.atombot.model.BotSettings;
import com.ejyqyl.atombot.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Intelligent NPC Training Dummy entity for AtomBot.
 * Features customizable gear, shield defense, natural humanoid stance,
 * and immortal combo training mechanics.
 *
 * @author ejyqyl
 */
public class TrainingBot {

    private final UUID botId = UUID.randomUUID();
    private final Plugin plugin;
    private BotSettings settings;
    private final Player owner;
    private final Location spawnLocation;
    private LivingEntity entity;
    private BukkitTask aiTask;
    private int attackCooldown = 0;

    public TrainingBot(Plugin plugin, BotSettings settings, Player owner, Location spawnLocation) {
        this.plugin = plugin;
        this.settings = settings;
        this.owner = owner;
        this.spawnLocation = spawnLocation;
    }

    public UUID getBotId() {
        return botId;
    }

    public Player getOwner() {
        return owner;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public BotSettings getSettings() {
        return settings;
    }

    public void spawn() {
        if (spawnLocation.getWorld() == null) return;

        Husk husk = (Husk) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.HUSK);
        this.entity = husk;

        husk.setAdult();
        husk.setSilent(true);
        husk.setRemoveWhenFarAway(false);
        husk.setCanBreakDoors(false);
        try {
            husk.setArmsRaised(false);
        } catch (Throwable ignored) {}

        if (husk.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            husk.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
            husk.setHealth(20.0);
        }

        applySettings(this.settings);
        updateHealthDisplay(20.0);
        startAiLoop();
    }

    public void applySettings(BotSettings newSettings) {
        this.settings = newSettings;
        if (!(entity instanceof Husk husk) || !husk.isValid()) return;

        EntityEquipment eq = husk.getEquipment();
        if (eq == null) return;

        // Helmet or Player Skin Head
        if (settings.getHelmet() != BotSettings.ArmorTier.NONE) {
            eq.setHelmet(settings.createHelmetItem());
        } else {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            skull.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(owner));
            eq.setHelmet(skull);
        }

        eq.setChestplate(settings.getChestplate() != BotSettings.ArmorTier.NONE ? settings.createChestplateItem() : null);
        eq.setLeggings(settings.getLeggings() != BotSettings.ArmorTier.NONE ? settings.createLeggingsItem() : null);
        eq.setBoots(settings.getBoots() != BotSettings.ArmorTier.NONE ? settings.createBootsItem() : null);

        // Shield in offhand
        if (settings.isUseShield()) {
            eq.setItemInOffHand(new ItemStack(Material.SHIELD));
        } else {
            eq.setItemInOffHand(null);
        }

        // Sword in mainhand
        if (settings.isAttackPlayer()) {
            eq.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
        } else {
            eq.setItemInMainHand(null);
        }

        eq.setHelmetDropChance(0.0f);
        eq.setChestplateDropChance(0.0f);
        eq.setLeggingsDropChance(0.0f);
        eq.setBootsDropChance(0.0f);
        eq.setItemInMainHandDropChance(0.0f);
        eq.setItemInOffHandDropChance(0.0f);

        // Movement Speed
        if (husk.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            double speed = settings.isFollowOwner() ? 0.25 : 0.0;
            husk.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        }
        if (!settings.isFollowOwner()) {
            husk.setTarget(null);
        }

        // Potion effects
        if (settings.isSlowFalling()) {
            husk.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, PotionEffect.INFINITE_DURATION, 0, false, false));
        } else {
            husk.removePotionEffect(PotionEffectType.SLOW_FALLING);
        }

        if (settings.isBlastResistance()) {
            husk.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 2, false, false));
        } else {
            husk.removePotionEffect(PotionEffectType.RESISTANCE);
        }

        try {
            husk.setArmsRaised(false);
        } catch (Throwable ignored) {}
    }

    private void startAiLoop() {
        aiTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || entity.isDead() || !entity.isValid() || owner == null || !owner.isOnline()) {
                    cancel();
                    return;
                }

                Location botLoc = entity.getLocation();
                Location oppLoc = owner.getLocation();

                if (!botLoc.getWorld().equals(oppLoc.getWorld())) {
                    cancel();
                    return;
                }

                double dist = botLoc.distance(oppLoc);

                if (attackCooldown > 0) {
                    attackCooldown--;
                }

                if (settings.isFollowOwner()) {
                    if (dist > 3.0) {
                        if (entity instanceof Husk h) h.setTarget(owner);
                    } else {
                        if (entity instanceof Husk h) h.setTarget(null);
                    }
                } else {
                    Vector dir = oppLoc.toVector().subtract(botLoc.toVector()).normalize();
                    botLoc.setDirection(dir);
                    entity.teleport(botLoc);
                    if (entity instanceof Husk h) h.setTarget(null);
                }

                if (entity instanceof Husk h) {
                    try { h.setArmsRaised(false); } catch (Throwable ignored) {}
                }

                if (settings.isAttackPlayer() && dist <= 3.0 && attackCooldown <= 0) {
                    entity.swingMainHand();
                    owner.damage(4.0, entity);
                    attackCooldown = 20;
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    public void playShieldBlock() {
        if (entity != null && entity.isValid()) {
            entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
            entity.getWorld().spawnParticle(Particle.CRIT, entity.getEyeLocation().subtract(0, 0.3, 0), 8, 0.2, 0.2, 0.2, 0.05);
            try {
                if (entity instanceof LivingEntity living) {
                    living.startUsingItem(org.bukkit.inventory.EquipmentSlot.OFF_HAND);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (living.isValid()) living.completeUsingActiveItem();
                    }, 10L);
                }
            } catch (Throwable ignored) {}
        }
    }

    public void playDamageReaction(double damage) {
        if (entity != null && entity.isValid()) {
            double newHp = Math.max(0.0, entity.getHealth() - damage);
            updateHealthDisplay(newHp);

            if (newHp <= 0.0) {
                entity.getWorld().spawnParticle(Particle.POOF, entity.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.05);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_DEATH, 0.8f, 1.2f);
                entity.setHealth(20.0);
                updateHealthDisplay(20.0);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (entity != null && entity.isValid()) {
                        updateHealthDisplay(entity.getHealth());
                    }
                }, 1L);
            }
        }
    }

    public void updateHealthDisplay(double hp) {
        if (entity == null || !entity.isValid()) return;
        String name = "<gradient:#00B5FD:#7670E5>NPC Бот</gradient> &c❤ " + String.format("%.1f", Math.max(0.0, hp));
        entity.customName(ColorUtil.parse(name));
        entity.setCustomNameVisible(true);
    }

    public void remove() {
        if (aiTask != null) {
            aiTask.cancel();
            aiTask = null;
        }
        if (entity != null && !entity.isDead()) {
            entity.getWorld().spawnParticle(Particle.POOF, entity.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
            entity.remove();
            entity = null;
        }
    }
}
