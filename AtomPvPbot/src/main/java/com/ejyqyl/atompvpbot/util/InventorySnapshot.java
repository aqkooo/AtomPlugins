package com.ejyqyl.atompvpbot.util;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Captures and cleanly restores player state before/after arena combat sessions.
 *
 * @author ejyqyl
 */
public class InventorySnapshot {

    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final ItemStack[] extra;
    private final double health;
    private final int food;
    private final float exp;
    private final int level;
    private final GameMode gameMode;
    private final Collection<PotionEffect> effects;

    public InventorySnapshot(Player player) {
        this.contents = cloneItemArray(player.getInventory().getContents());
        this.armor = cloneItemArray(player.getInventory().getArmorContents());
        this.extra = cloneItemArray(player.getInventory().getExtraContents());
        this.health = player.getHealth();
        this.food = player.getFoodLevel();
        this.exp = player.getExp();
        this.level = player.getLevel();
        this.gameMode = player.getGameMode();
        this.effects = new ArrayList<>(player.getActivePotionEffects());
    }

    public void restore(Player player) {
        if (player == null || !player.isOnline()) return;

        player.getInventory().clear();
        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(armor);
        player.getInventory().setExtraContents(extra);

        player.setGameMode(gameMode);
        player.setHealth(Math.min(player.getMaxHealth(), Math.max(1.0, health)));
        player.setFoodLevel(food);
        player.setExp(exp);
        player.setLevel(level);

        for (PotionEffect pe : player.getActivePotionEffects()) {
            player.removePotionEffect(pe.getType());
        }
        for (PotionEffect pe : effects) {
            player.addPotionEffect(pe);
        }
    }

    private ItemStack[] cloneItemArray(ItemStack[] original) {
        if (original == null) return new ItemStack[0];
        ItemStack[] copy = new ItemStack[original.length];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i] != null ? original[i].clone() : null;
        }
        return copy;
    }
}
