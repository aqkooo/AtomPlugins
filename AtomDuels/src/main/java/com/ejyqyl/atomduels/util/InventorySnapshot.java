package com.ejyqyl.atomduels.util;

import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Complete snapshot of a player's inventory, status, armor, and potion effects.
 * Provides safe restoration and kit item cleanup.
 * Authored by ejyqyl.
 */
public class InventorySnapshot {

    public static final NamespacedKey KIT_ITEM_KEY = new NamespacedKey("atomduels", "kit_item");

    private final UUID playerUuid;
    private final ItemStack[] contents;
    private final ItemStack[] armorContents;
    private final ItemStack[] extraContents;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int expLevel;
    private final float exp;
    private final GameMode gameMode;
    private final Collection<PotionEffect> potionEffects;
    private final int fireTicks;

    public InventorySnapshot(Player player) {
        this.playerUuid = player.getUniqueId();
        this.contents = cloneItemArray(player.getInventory().getStorageContents());
        this.armorContents = cloneItemArray(player.getInventory().getArmorContents());
        this.extraContents = cloneItemArray(player.getInventory().getExtraContents());
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.expLevel = player.getLevel();
        this.exp = player.getExp();
        this.gameMode = player.getGameMode();
        this.potionEffects = new ArrayList<>(player.getActivePotionEffects());
        this.fireTicks = player.getFireTicks();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public ItemStack[] getContents() {
        return cloneItemArray(contents);
    }

    public ItemStack[] getArmorContents() {
        return cloneItemArray(armorContents);
    }

    public ItemStack[] getExtraContents() {
        return cloneItemArray(extraContents);
    }

    /**
     * Tags an item as a duel kit item using PersistentDataContainer.
     */
    public static ItemStack tagKitItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return item;
        }
        ItemStack clone = item.clone();
        clone.editMeta(meta -> meta.getPersistentDataContainer().set(KIT_ITEM_KEY, PersistentDataType.BYTE, (byte) 1));
        return clone;
    }

    /**
     * Checks if an item is tagged as a kit item.
     */
    public static boolean isKitItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(KIT_ITEM_KEY, PersistentDataType.BYTE);
    }

    /**
     * Strips all kit items from player's inventory.
     */
    public static void clearKitItems(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isKitItem(contents[i])) {
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);
    }

    /**
     * Restores the snapshot to the player.
     * Any items that do not fit into the player's inventory are returned as overflow items.
     *
     * @param player the player to restore
     * @return list of items that could not fit in the inventory
     */
    public List<ItemStack> restore(Player player) {
        // Clear all active potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Wipe kit items and clear inventory completely
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setExtraContents(new ItemStack[1]);

        // Restore status
        player.setGameMode(gameMode);
        try {
            player.setHealth(Math.min(health, player.getMaxHealth()));
        } catch (Exception e) {
            player.setHealth(20.0);
        }
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setLevel(expLevel);
        player.setExp(exp);
        player.setFireTicks(fireTicks);

        // Reapply potion effects
        for (PotionEffect effect : potionEffects) {
            player.addPotionEffect(effect);
        }

        // Restore armor and extra
        player.getInventory().setArmorContents(cloneItemArray(armorContents));
        player.getInventory().setExtraContents(cloneItemArray(extraContents));

        // Restore contents and track overflow
        List<ItemStack> overflow = new ArrayList<>();
        if (contents != null) {
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item != null && !item.getType().isAir()) {
                    if (i < player.getInventory().getSize()) {
                        player.getInventory().setItem(i, item.clone());
                    } else {
                        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(item.clone());
                        overflow.addAll(remaining.values());
                    }
                }
            }
        }

        return overflow;
    }

    private static ItemStack[] cloneItemArray(ItemStack[] source) {
        if (source == null) {
            return new ItemStack[0];
        }
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            if (source[i] != null) {
                copy[i] = source[i].clone();
            }
        }
        return copy;
    }
}
