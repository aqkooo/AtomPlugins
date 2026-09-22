package com.ejyqyl.atompvpbot.entity;

import com.ejyqyl.atompvpbot.ai.BotAI;
import com.ejyqyl.atompvpbot.ai.BotBehaviorMode;
import com.ejyqyl.atompvpbot.ai.BotDifficulty;
import com.ejyqyl.atompvpbot.ai.CombatSystem;
import com.ejyqyl.atompvpbot.ai.CustomAIProfile;
import com.ejyqyl.atompvpbot.ai.DefenseSystem;
import com.ejyqyl.atompvpbot.ai.InventorySystem;
import com.ejyqyl.atompvpbot.ai.MovementSystem;
import com.ejyqyl.atompvpbot.ai.RotationSystem;
import com.ejyqyl.atompvpbot.ai.SurvivalSystem;
import com.ejyqyl.atompvpbot.ai.TargetingSystem;
import com.ejyqyl.atompvpbot.ai.UtilitySystem;
import com.ejyqyl.atompvpbot.arena.Arena;
import com.ejyqyl.atompvpbot.kit.BotKit;
import com.ejyqyl.atompvpbot.util.ColorUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * High-fidelity humanoid PvP Bot entity representation.
 *
 * @author ejyqyl
 */
public class PvPBotEntity {

    public static final String BOT_METADATA_KEY = "AtomPvPbot_Entity";

    private final Plugin plugin;
    private final UUID id = UUID.randomUUID();
    private Husk mob;
    private Location spawnLocation;
    private Arena arena;
    private Player fightOpponent;
    private BotDifficulty difficulty = BotDifficulty.NORMAL;
    private BotBehaviorMode behaviorMode = BotBehaviorMode.BALANCED;
    private CustomAIProfile customProfile = new CustomAIProfile();
    private BotKit kit;

    private BotAI ai;
    private boolean infiniteTotemMode = true;

    public PvPBotEntity(Plugin plugin) {
        this.plugin = plugin;
    }

    public void spawn(Location location) {
        this.spawnLocation = location.clone();
        if (location.getWorld() == null) return;

        this.mob = (Husk) location.getWorld().spawnEntity(location, EntityType.HUSK);
        mob.setMetadata(BOT_METADATA_KEY, new FixedMetadataValue(plugin, this));

        // Player-like characteristics
        mob.setSilent(true);
        mob.setAdult();
        mob.setCanBreakDoors(false);
        mob.setArmsRaised(false);
        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);

        // Attributes
        AttributeInstance maxHealth = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(20.0);
        mob.setHealth(20.0);

        AttributeInstance followRange = mob.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
        if (followRange != null) followRange.setBaseValue(48.0);

        AttributeInstance movementSpeed = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed != null) movementSpeed.setBaseValue(0.28); // Standard player walk speed

        mob.setCustomNameVisible(true);
        updateNameTag();

        // Initialize AI
        this.ai = new BotAI(this);

        // Apply Kit if present
        if (kit != null) {
            applyKit(kit);
        } else {
            equipDefaultGear();
        }
    }

    public void updateNameTag() {
        if (mob == null || !mob.isValid()) return;
        double hp = Math.max(0, mob.getHealth());
        String hpColor = hp > 12.0 ? "<#55FF55>" : (hp > 6.0 ? "<#FFAA00>" : "<#FF5555>");
        String tag = "<gradient:#00B5FD:#7670E5>PvP Бот</gradient> &7[" + hpColor + String.format("%.1f", hp) + "❤&7]";
        mob.customName(ColorUtil.parse(tag));
    }

    public void applyKit(BotKit kit) {
        this.kit = kit;
        if (mob == null || !mob.isValid() || kit == null) return;
        kit.applyTo(mob);

        // Populate hotbar in inventory system
        if (ai != null) {
            for (int i = 0; i < 9; i++) {
                ItemStack item = kit.getItems().get(i);
                ai.getInventorySystem().setItem(i, item);
            }
            ai.getInventorySystem().restoreDefaultWeapon();
        }

        // Apply player skull if no helmet is specified
        if (mob.getEquipment() != null && (mob.getEquipment().getHelmet() == null || mob.getEquipment().getHelmet().getType().isAir())) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                if (fightOpponent != null) {
                    meta.setOwningPlayer(fightOpponent);
                }
                skull.setItemMeta(meta);
            }
            mob.getEquipment().setHelmet(skull);
        }

        // Ensure infinite totem in offhand for practice mode if configured
        if (infiniteTotemMode && mob.getEquipment() != null) {
            mob.getEquipment().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
        }
    }

    private void equipDefaultGear() {
        if (mob == null || mob.getEquipment() == null) return;
        mob.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
        mob.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        mob.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        mob.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        mob.getEquipment().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));

        if (fightOpponent != null) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(fightOpponent);
                skull.setItemMeta(meta);
            }
            mob.getEquipment().setHelmet(skull);
        }
    }

    public void tick() {
        if (ai != null) {
            ai.tick();
        }
    }

    public void despawn() {
        if (mob != null && mob.isValid()) {
            mob.removeMetadata(BOT_METADATA_KEY, plugin);
            mob.remove();
        }
        mob = null;
    }

    public void reset() {
        if (mob != null && mob.isValid() && spawnLocation != null) {
            mob.teleport(spawnLocation);
            mob.setHealth(20.0);
            updateNameTag();
            if (kit != null) {
                applyKit(kit);
            } else {
                equipDefaultGear();
            }
        }
    }

    // Effective parameters based on difficulty or custom profile
    public int getEffectiveCps() {
        return difficulty == BotDifficulty.CUSTOM ? customProfile.getCps() : difficulty.getCps();
    }

    public double getEffectiveReach() {
        return difficulty == BotDifficulty.CUSTOM ? customProfile.getReach() : difficulty.getReach();
    }

    public float getEffectiveAimSpeed() {
        return difficulty == BotDifficulty.CUSTOM ? customProfile.getAimSpeed() : difficulty.getAimSpeed();
    }

    public double getEffectiveAimAccuracy() {
        return difficulty == BotDifficulty.CUSTOM ? customProfile.getAimAccuracy() : difficulty.getAimAccuracy();
    }

    public double getEffectiveShieldBlockChance() {
        return difficulty == BotDifficulty.CUSTOM ? customProfile.getShieldBlockChance() : difficulty.getShieldBlockChance();
    }

    public double getEffectiveJumpCritChance() {
        return difficulty == BotDifficulty.CUSTOM ? customProfile.getJumpCritChance() : difficulty.getJumpCritChance();
    }

    public int getEffectiveComboMaxHits() {
        return difficulty == BotDifficulty.CUSTOM ? customProfile.getComboMaxHits() : difficulty.getComboMaxHits();
    }

    public boolean isSprintResetEnabled() {
        return difficulty == BotDifficulty.CUSTOM ? customProfile.isSprintResetCombo() : difficulty.isSprintResetCombo();
    }

    public boolean canBreakShieldWithAxe() {
        return difficulty == BotDifficulty.CUSTOM ? customProfile.isAxeShieldBreak() : difficulty.isAxeShieldBreak();
    }

    public boolean canMaceSmash() {
        return difficulty == BotDifficulty.CUSTOM ? customProfile.isMaceSmash() : difficulty.isMaceSmash();
    }

    public boolean canPearlClutch() {
        return difficulty == BotDifficulty.CUSTOM ? customProfile.isPearlClutch() : difficulty.isPearlClutch();
    }

    public boolean canUsePotions() {
        return difficulty == BotDifficulty.CUSTOM ? customProfile.isPotionUse() : difficulty.isPotionUse();
    }

    // Subsystem shortcuts
    public TargetingSystem getTargetingSystem() { return ai != null ? ai.getTargetingSystem() : null; }
    public MovementSystem getMovementSystem() { return ai != null ? ai.getMovementSystem() : null; }
    public CombatSystem getCombatSystem() { return ai != null ? ai.getCombatSystem() : null; }
    public RotationSystem getRotationSystem() { return ai != null ? ai.getRotationSystem() : null; }
    public InventorySystem getInventorySystem() { return ai != null ? ai.getInventorySystem() : null; }
    public DefenseSystem getDefenseSystem() { return ai != null ? ai.getDefenseSystem() : null; }
    public UtilitySystem getUtilitySystem() { return ai != null ? ai.getUtilitySystem() : null; }
    public SurvivalSystem getSurvivalSystem() { return ai != null ? ai.getSurvivalSystem() : null; }

    public UUID getId() { return id; }
    public Husk getMob() { return mob; }
    public Location getSpawnLocation() { return spawnLocation; }
    public void setSpawnLocation(Location loc) { this.spawnLocation = loc; }
    public Arena getArena() { return arena; }
    public void setArena(Arena arena) { this.arena = arena; }
    public Player getFightOpponent() { return fightOpponent; }
    public void setFightOpponent(Player player) { this.fightOpponent = player; }
    public BotDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(BotDifficulty difficulty) { this.difficulty = difficulty; }
    public BotBehaviorMode getBehaviorMode() { return behaviorMode; }
    public void setBehaviorMode(BotBehaviorMode behaviorMode) { this.behaviorMode = behaviorMode; }
    public CustomAIProfile getCustomProfile() { return customProfile; }
    public void setCustomProfile(CustomAIProfile customProfile) { this.customProfile = customProfile; }
    public BotKit getKit() { return kit; }
    public void setKit(BotKit kit) { this.kit = kit; }
    public boolean isInfiniteTotemMode() { return infiniteTotemMode; }
    public void setInfiniteTotemMode(boolean infiniteTotemMode) { this.infiniteTotemMode = infiniteTotemMode; }
}
