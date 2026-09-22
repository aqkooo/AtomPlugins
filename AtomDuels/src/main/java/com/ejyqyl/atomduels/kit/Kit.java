package com.ejyqyl.atomduels.kit;

import com.ejyqyl.atomduels.util.InventorySnapshot;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Duel Kit representation containing equipment, inventory layout, and effects.
 * Every item given to a player is tagged with PersistentDataContainer to prevent item smuggling.
 * Authored by ejyqyl.
 */
public class Kit {

    private final String name;
    private String displayName;
    private Material icon = Material.DIAMOND_SWORD;
    private ItemStack[] items = new ItemStack[36];
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack offhand;
    private final List<PotionEffect> effects = new ArrayList<>();

    public Kit(String name) {
        this.name = name;
        this.displayName = name;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Material getIcon() {
        return icon != null ? icon : Material.DIAMOND_SWORD;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public ItemStack[] getItems() {
        return items;
    }

    public void setItems(ItemStack[] items) {
        this.items = items;
    }

    public ItemStack getHelmet() {
        return helmet;
    }

    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet;
    }

    public ItemStack getChestplate() {
        return chestplate;
    }

    public void setChestplate(ItemStack chestplate) {
        this.chestplate = chestplate;
    }

    public ItemStack getLeggings() {
        return leggings;
    }

    public void setLeggings(ItemStack leggings) {
        this.leggings = leggings;
    }

    public ItemStack getBoots() {
        return boots;
    }

    public void setBoots(ItemStack boots) {
        this.boots = boots;
    }

    public ItemStack getOffhand() {
        return offhand;
    }

    public void setOffhand(ItemStack offhand) {
        this.offhand = offhand;
    }

    public List<PotionEffect> getEffects() {
        return effects;
    }

    public void setEffects(List<PotionEffect> effects) {
        this.effects.clear();
        if (effects != null) {
            this.effects.addAll(effects);
        }
    }

    /**
     * Applies this kit to a player, tagging all items with kit PDC tags.
     */
    public void apply(Player player) {
        player.getInventory().clear();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        if (items != null) {
            for (int i = 0; i < items.length && i < 36; i++) {
                if (items[i] != null && !items[i].getType().isAir()) {
                    player.getInventory().setItem(i, InventorySnapshot.tagKitItem(items[i]));
                }
            }
        }

        ItemStack[] armor = new ItemStack[] {
            boots != null ? InventorySnapshot.tagKitItem(boots) : null,
            leggings != null ? InventorySnapshot.tagKitItem(leggings) : null,
            chestplate != null ? InventorySnapshot.tagKitItem(chestplate) : null,
            helmet != null ? InventorySnapshot.tagKitItem(helmet) : null
        };
        player.getInventory().setArmorContents(armor);

        if (offhand != null) {
            player.getInventory().setItemInOffHand(InventorySnapshot.tagKitItem(offhand));
        }

        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }

        player.updateInventory();
    }
}
