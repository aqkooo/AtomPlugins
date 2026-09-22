package ru.glowdev.glowsnake.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private final ItemStack itemStack;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material != null ? material : Material.STONE);
    }

    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material != null ? material : Material.STONE, Math.max(1, amount));
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack != null ? itemStack.clone() : new ItemStack(Material.STONE);
    }

    public static ItemBuilder from(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder from(ItemStack itemStack) {
        return new ItemBuilder(itemStack);
    }

    public ItemBuilder name(String displayName) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtil.formatItemText(displayName));
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    public ItemBuilder lore(List<String> lines) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setLore(ChatUtil.formatItemLore(lines));
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        itemStack.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(flags);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder hideAllFlags() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return itemStack.clone();
    }
}
