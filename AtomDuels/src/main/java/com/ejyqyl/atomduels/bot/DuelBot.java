package com.ejyqyl.atomduels.bot;

import com.ejyqyl.atomduels.kit.Kit;
import com.ejyqyl.atomduels.util.ColorUtil;
import net.kyori.adventure.text.Component;
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

import java.util.Random;
import java.util.UUID;

/**
 * Intelligent NPC Combat & Training Bot.
 * Supports configurable movement, shield defense, slow falling, blast resistance, and full combat mechanics.
 * Authored by ejyqyl (https://github.com/aqkooo).
 */
public class DuelBot {

    private final UUID botId = UUID.randomUUID();
    private final Plugin plugin;
    private final BotProfile profile;
    private BotSettings settings;
    private final boolean trainingBot;
    private final Player opponent;
    private final Location spawnLocation;
    private final Kit kit;
    private LivingEntity entity;
    private BukkitTask aiTask;
    private final Random random = new Random();

    private int attackCooldown = 0;
    private int strafeTicks = 0;
    private boolean strafeLeft = true;
    private int potionsLeft = 4;
    private int pearlsLeft = 2;
    private int totemsLeft = 1;

    /**
     * Duel match bot constructor.
     */
    public DuelBot(Plugin plugin, BotProfile profile, Player opponent, Location spawnLocation, Kit kit) {
        this.plugin = plugin;
        this.profile = profile;
        this.settings = null;
        this.trainingBot = false;
        this.opponent = opponent;
        this.spawnLocation = spawnLocation;
        this.kit = kit;
    }

    /**
     * Training NPC bot constructor based on custom player settings.
     */
    public DuelBot(Plugin plugin, BotSettings settings, Player owner, Location spawnLocation) {
        this.plugin = plugin;
        this.profile = null;
        this.settings = settings;
        this.trainingBot = true;
        this.opponent = owner;
        this.spawnLocation = spawnLocation;
        this.kit = null;
    }

    public UUID getBotId() {
        return botId;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public BotProfile getProfile() {
        return profile;
    }

    public BotSettings getSettings() {
        return settings;
    }

    public boolean isTrainingBot() {
        return trainingBot;
    }

    public void spawn() {
        if (spawnLocation.getWorld() == null) return;

        // Husk doesn't burn in sun, supports full equipment, completely silent
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

        if (trainingBot) {
            setupTrainingBot(husk);
        } else {
            setupDuelBot(husk);
        }

        updateHealthDisplay(20.0);
        startAiLoop();
    }

    public void applySettings(BotSettings newSettings) {
        this.settings = newSettings;
        if (entity instanceof Husk husk && husk.isValid()) {
            setupTrainingBot(husk);
        }
    }

    private void setupTrainingBot(Husk husk) {
        EntityEquipment eq = husk.getEquipment();
        if (eq == null) return;

        // Helmet or Player Head
        if (settings.getHelmet() != BotSettings.ArmorTier.NONE) {
            eq.setHelmet(settings.createHelmetItem());
        } else {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            skull.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(opponent));
            eq.setHelmet(skull);
        }

        eq.setChestplate(settings.getChestplate() != BotSettings.ArmorTier.NONE ? settings.createChestplateItem() : null);
        eq.setLeggings(settings.getLeggings() != BotSettings.ArmorTier.NONE ? settings.createLeggingsItem() : null);
        eq.setBoots(settings.getBoots() != BotSettings.ArmorTier.NONE ? settings.createBootsItem() : null);

        if (settings.isUseShield()) {
            eq.setItemInOffHand(new ItemStack(Material.SHIELD));
        } else {
            eq.setItemInOffHand(null);
        }

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

    private void setupDuelBot(Husk husk) {
        if (husk.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            double speed = 0.28 + (profile.getDifficulty().ordinal() * 0.03);
            husk.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        }

        EntityEquipment eq = husk.getEquipment();
        if (eq == null) return;

        if (kit != null) {
            if (kit.getHelmet() != null) eq.setHelmet(kit.getHelmet().clone());
            else {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                skull.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(opponent));
                eq.setHelmet(skull);
            }
            if (kit.getChestplate() != null) eq.setChestplate(kit.getChestplate().clone());
            if (kit.getLeggings() != null) eq.setLeggings(kit.getLeggings().clone());
            if (kit.getBoots() != null) eq.setBoots(kit.getBoots().clone());
            if (kit.getOffhand() != null) eq.setItemInOffHand(kit.getOffhand().clone());

            ItemStack[] items = kit.getItems();
            if (items != null && items.length > 0 && items[0] != null) {
                eq.setItemInMainHand(items[0].clone());
            } else {
                eq.setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
            }
        } else {
            eq.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            eq.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            eq.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            eq.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            eq.setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
            eq.setItemInOffHand(new ItemStack(Material.SHIELD));
        }

        eq.setHelmetDropChance(0.0f);
        eq.setChestplateDropChance(0.0f);
        eq.setLeggingsDropChance(0.0f);
        eq.setBootsDropChance(0.0f);
        eq.setItemInMainHandDropChance(0.0f);
        eq.setItemInOffHandDropChance(0.0f);
    }

    private void startAiLoop() {
        aiTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || entity.isDead() || !entity.isValid() || opponent == null || !opponent.isOnline()) {
                    cancel();
                    return;
                }

                Location botLoc = entity.getLocation();
                Location oppLoc = opponent.getLocation();

                if (!botLoc.getWorld().equals(oppLoc.getWorld())) {
                    cancel();
                    return;
                }

                double dist = botLoc.distance(oppLoc);

                if (attackCooldown > 0) {
                    attackCooldown--;
                }

                if (trainingBot) {
                    // Training Bot Logic
                    if (settings.isFollowOwner()) {
                        if (dist > 3.0) {
                            if (entity instanceof Husk h) h.setTarget(opponent);
                        } else {
                            if (entity instanceof Husk h) h.setTarget(null);
                        }
                    } else {
                        // Static dummy looking at owner
                        Vector dir = oppLoc.toVector().subtract(botLoc.toVector()).normalize();
                        botLoc.setDirection(dir);
                        entity.teleport(botLoc);
                        if (entity instanceof Husk h) h.setTarget(null);
                    }

                    if (settings.isAttackPlayer() && dist <= 3.0 && attackCooldown <= 0) {
                        entity.swingMainHand();
                        opponent.damage(4.0, entity);
                        attackCooldown = 20;
                    }
                    return;
                }

                // Duel Match AI Logic
                if (entity instanceof Husk husk) {
                    husk.setTarget(opponent);
                }

                // Attack swing
                if (dist <= 3.2 && attackCooldown <= 0) {
                    if (random.nextDouble() <= profile.getAimAccuracy()) {
                        entity.swingMainHand();
                        opponent.damage(5.0 + profile.getDifficulty().ordinal(), entity);
                        attackCooldown = profile.getAttackDelayTicks();
                    }
                }

                // Strafing
                if (dist <= 8.0 && random.nextDouble() < profile.getStrafeChance()) {
                    strafeTicks++;
                    if (strafeTicks > 15) {
                        strafeTicks = 0;
                        strafeLeft = !strafeLeft;
                    }
                    Vector dir = oppLoc.toVector().subtract(botLoc.toVector()).normalize();
                    Vector ortho = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
                    if (!strafeLeft) {
                        ortho.multiply(-1);
                    }
                    Vector currentVel = entity.getVelocity();
                    entity.setVelocity(currentVel.add(ortho.multiply(0.08)));
                }

                // Potion healing
                if (entity.getHealth() < 10.0 && potionsLeft > 0 && random.nextDouble() < profile.getPotionHealChance()) {
                    potionsLeft--;
                    entity.setHealth(Math.min(entity.getMaxHealth(), entity.getHealth() + 6.0));
                    entity.getWorld().spawnParticle(Particle.HEART, entity.getEyeLocation(), 5, 0.3, 0.3, 0.3, 0.1);
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 1.0f, 1.0f);
                    updateHealthDisplay(entity.getHealth());
                }

                // Pearl clutching
                if (dist > 14.0 && pearlsLeft > 0 && random.nextDouble() < profile.getPearlClutchChance()) {
                    pearlsLeft--;
                    Vector toOpp = oppLoc.toVector().subtract(botLoc.toVector()).normalize();
                    Location clutchLoc = oppLoc.clone().subtract(toOpp.multiply(3.0));
                    entity.teleport(clutchLoc);
                    entity.getWorld().playSound(clutchLoc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);
                    entity.getWorld().spawnParticle(Particle.PORTAL, clutchLoc, 20, 0.5, 0.5, 0.5, 0.1);
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

            // If training bot dies, restore health for continuous training!
            if (trainingBot && newHp <= 0.0) {
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

        String prefix = trainingBot ? "<gradient:#00B5FD:#7670E5>NPC Бот</gradient>" : "<gradient:#00B5FD:#7670E5>Бот • " + profile.getDifficulty().getDisplayName() + "</gradient>";
        String name = prefix + " &c❤ " + String.format("%.1f", Math.max(0.0, hp));
        entity.customName(ColorUtil.parse(name));
        entity.setCustomNameVisible(true);
    }

    public boolean handleFatalDamage() {
        if (trainingBot) {
            entity.setHealth(20.0);
            updateHealthDisplay(20.0);
            return true;
        }

        if (totemsLeft > 0 && random.nextDouble() < profile.getTotemHoldChance()) {
            totemsLeft--;
            if (entity != null) {
                entity.setHealth(4.0);
                entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
                entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                entity.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, entity.getEyeLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                updateHealthDisplay(4.0);
            }
            return true;
        }
        return false;
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
