package com.ejyqyl.atompvpbot.ai;

import com.ejyqyl.atompvpbot.entity.PvPBotEntity;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages bot hotbar simulation, item selection, and weapon switching.
 *
 * @author ejyqyl
 */
public class InventorySystem {

    private final PvPBotEntity bot;
    private final Map<Integer, ItemStack> hotbar = new HashMap<>();
    private int activeSlot = 0;

    public InventorySystem(PvPBotEntity bot) {
        this.bot = bot;
    }

    public void setItem(int slot, ItemStack item) {
        if (item != null) {
            hotbar.put(slot, item.clone());
        } else {
            hotbar.remove(slot);
        }
    }

    public ItemStack getItem(int slot) {
        return hotbar.get(slot);
    }

    public void switchToSlot(int slot) {
        this.activeSlot = slot;
        Mob mob = bot.getMob();
        if (mob == null || !mob.isValid()) return;

        EntityEquipment eq = mob.getEquipment();
        if (eq == null) return;

        ItemStack item = hotbar.get(slot);
        eq.setItemInMainHand(item != null ? item.clone() : new ItemStack(Material.AIR));
    }

    public boolean switchToWeapon(Material weaponType) {
        for (Map.Entry<Integer, ItemStack> entry : hotbar.entrySet()) {
            if (entry.getValue() != null && entry.getValue().getType() == weaponType) {
                switchToSlot(entry.getKey());
                return true;
            }
        }
        return false;
    }

    public boolean switchToAxe() {
        for (Map.Entry<Integer, ItemStack> entry : hotbar.entrySet()) {
            if (entry.getValue() != null && entry.getValue().getType().name().endsWith("_AXE")) {
                switchToSlot(entry.getKey());
                return true;
            }
        }
        return false;
    }

    public boolean switchToSword() {
        for (Map.Entry<Integer, ItemStack> entry : hotbar.entrySet()) {
            if (entry.getValue() != null && entry.getValue().getType().name().endsWith("_SWORD")) {
                switchToSlot(entry.getKey());
                return true;
            }
        }
        return false;
    }

    public boolean switchToMace() {
        for (Map.Entry<Integer, ItemStack> entry : hotbar.entrySet()) {
            if (entry.getValue() != null && entry.getValue().getType() == Material.MACE) {
                switchToSlot(entry.getKey());
                return true;
            }
        }
        return false;
    }

    public boolean switchToBow() {
        for (Map.Entry<Integer, ItemStack> entry : hotbar.entrySet()) {
            if (entry.getValue() != null && (entry.getValue().getType() == Material.BOW || entry.getValue().getType() == Material.CROSSBOW)) {
                switchToSlot(entry.getKey());
                return true;
            }
        }
        return false;
    }

    public boolean hasItem(Material material) {
        for (ItemStack item : hotbar.values()) {
            if (item != null && item.getType() == material) return true;
        }
        return false;
    }

    public void restoreDefaultWeapon() {
        // Prefer sword, then mace, then axe, then slot 0
        if (switchToSword()) return;
        if (switchToMace()) return;
        if (switchToAxe()) return;
        switchToSlot(0);
    }

    public Map<Integer, ItemStack> getHotbar() {
        return hotbar;
    }

    public int getActiveSlot() {
        return activeSlot;
    }
}
