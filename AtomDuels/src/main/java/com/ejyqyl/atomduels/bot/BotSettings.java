package com.ejyqyl.atomduels.bot;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Customizable parameters for the training NPC bot based on user configuration.
 * Authored by ejyqyl (https://github.com/aqkooo).
 */
public class BotSettings {

    public enum ArmorTier {
        NONE("Без брони", Material.BARRIER, null, null, null, null),
        LEATHER("Кожаный", Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS),
        IRON("Железный", Material.IRON_CHESTPLATE, Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS),
        DIAMOND("Алмазный", Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS),
        NETHERITE("Незеритовый", Material.NETHERITE_CHESTPLATE, Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS);

        private final String displayName;
        private final Material icon;
        private final Material helmet;
        private final Material chestplate;
        private final Material leggings;
        private final Material boots;

        ArmorTier(String displayName, Material icon, Material helmet, Material chestplate, Material leggings, Material boots) {
            this.displayName = displayName;
            this.icon = icon;
            this.helmet = helmet;
            this.chestplate = chestplate;
            this.leggings = leggings;
            this.boots = boots;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }

        public Material getHelmet() {
            return helmet;
        }

        public Material getChestplate() {
            return chestplate;
        }

        public Material getLeggings() {
            return leggings;
        }

        public Material getBoots() {
            return boots;
        }

        public ArmorTier next() {
            ArmorTier[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private ArmorTier helmet = ArmorTier.NETHERITE;
    private ArmorTier chestplate = ArmorTier.NETHERITE;
    private ArmorTier leggings = ArmorTier.NETHERITE;
    private ArmorTier boots = ArmorTier.NETHERITE;

    private boolean blastResistance = false;
    private boolean followOwner = false;
    private boolean useShield = false;
    private boolean slowFalling = false;
    private boolean attackPlayer = false;

    public ArmorTier getHelmet() {
        return helmet;
    }

    public void setHelmet(ArmorTier helmet) {
        this.helmet = helmet;
    }

    public void nextHelmet() {
        this.helmet = this.helmet.next();
    }

    public ArmorTier getChestplate() {
        return chestplate;
    }

    public void setChestplate(ArmorTier chestplate) {
        this.chestplate = chestplate;
    }

    public void nextChestplate() {
        this.chestplate = this.chestplate.next();
    }

    public ArmorTier getLeggings() {
        return leggings;
    }

    public void setLeggings(ArmorTier leggings) {
        this.leggings = leggings;
    }

    public void nextLeggings() {
        this.leggings = this.leggings.next();
    }

    public ArmorTier getBoots() {
        return boots;
    }

    public void setBoots(ArmorTier boots) {
        this.boots = boots;
    }

    public void nextBoots() {
        this.boots = this.boots.next();
    }

    public boolean isBlastResistance() {
        return blastResistance;
    }

    public void setBlastResistance(boolean blastResistance) {
        this.blastResistance = blastResistance;
    }

    public void toggleBlastResistance() {
        this.blastResistance = !this.blastResistance;
    }

    public boolean isFollowOwner() {
        return followOwner;
    }

    public void setFollowOwner(boolean followOwner) {
        this.followOwner = followOwner;
    }

    public void toggleFollowOwner() {
        this.followOwner = !this.followOwner;
    }

    public boolean isUseShield() {
        return useShield;
    }

    public void setUseShield(boolean useShield) {
        this.useShield = useShield;
    }

    public void toggleUseShield() {
        this.useShield = !this.useShield;
    }

    public boolean isSlowFalling() {
        return slowFalling;
    }

    public void setSlowFalling(boolean slowFalling) {
        this.slowFalling = slowFalling;
    }

    public void toggleSlowFalling() {
        this.slowFalling = !this.slowFalling;
    }

    public boolean isAttackPlayer() {
        return attackPlayer;
    }

    public void setAttackPlayer(boolean attackPlayer) {
        this.attackPlayer = attackPlayer;
    }

    public void toggleAttackPlayer() {
        this.attackPlayer = !this.attackPlayer;
    }

    public ItemStack createHelmetItem() {
        return helmet.getHelmet() != null ? new ItemStack(helmet.getHelmet()) : null;
    }

    public ItemStack createChestplateItem() {
        return chestplate.getChestplate() != null ? new ItemStack(chestplate.getChestplate()) : null;
    }

    public ItemStack createLeggingsItem() {
        return leggings.getLeggings() != null ? new ItemStack(leggings.getLeggings()) : null;
    }

    public ItemStack createBootsItem() {
        return boots.getBoots() != null ? new ItemStack(boots.getBoots()) : null;
    }
}
