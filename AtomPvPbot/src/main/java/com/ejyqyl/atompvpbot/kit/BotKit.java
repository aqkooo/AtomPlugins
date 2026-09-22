package com.ejyqyl.atompvpbot.kit;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Combat kit model storing armor, main inventory slots, and off-hand items.
 *
 * @author ejyqyl
 */
public class BotKit {

    private final String name;
    private String displayName;
    private Material icon = Material.DIAMOND_SWORD;
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack offhand;
    private final Map<Integer, ItemStack> items = new HashMap<>();

    public BotKit(String name) {
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
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
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

    public Map<Integer, ItemStack> getItems() {
        return items;
    }

    public void applyTo(Player player) {
        if (player == null || !player.isOnline()) return;
        player.getInventory().clear();

        if (helmet != null) player.getInventory().setHelmet(helmet.clone());
        if (chestplate != null) player.getInventory().setChestplate(chestplate.clone());
        if (leggings != null) player.getInventory().setLeggings(leggings.clone());
        if (boots != null) player.getInventory().setBoots(boots.clone());
        if (offhand != null) player.getInventory().setItemInOffHand(offhand.clone());

        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            if (entry.getValue() != null && entry.getKey() >= 0 && entry.getKey() < 36) {
                player.getInventory().setItem(entry.getKey(), entry.getValue().clone());
            }
        }
        player.updateInventory();
    }

    public void applyTo(LivingEntity entity) {
        if (entity == null || !entity.isValid()) return;
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return;

        eq.setHelmet(helmet != null ? helmet.clone() : null);
        eq.setChestplate(chestplate != null ? chestplate.clone() : null);
        eq.setLeggings(leggings != null ? leggings.clone() : null);
        eq.setBoots(boots != null ? boots.clone() : null);
        eq.setItemInOffHand(offhand != null ? offhand.clone() : null);

        // First weapon in slot 0 or fallback
        ItemStack main = items.get(0);
        if (main != null) {
            eq.setItemInMainHand(main.clone());
        } else {
            eq.setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
        }

        eq.setHelmetDropChance(0.0f);
        eq.setChestplateDropChance(0.0f);
        eq.setLeggingsDropChance(0.0f);
        eq.setBootsDropChance(0.0f);
        eq.setItemInMainHandDropChance(0.0f);
        eq.setItemInOffHandDropChance(0.0f);
    }
}
