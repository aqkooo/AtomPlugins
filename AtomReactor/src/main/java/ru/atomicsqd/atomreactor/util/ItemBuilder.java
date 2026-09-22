package ru.atomicsqd.atomreactor.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material != null ? material : Material.STONE, Math.max(1, amount));
        this.meta = this.item.getItemMeta();
        if (this.meta != null) {
            try {
                this.meta.addItemFlags(
                        ItemFlag.HIDE_ATTRIBUTES,
                        ItemFlag.HIDE_ENCHANTS,
                        ItemFlag.HIDE_UNBREAKABLE,
                        ItemFlag.HIDE_ARMOR_TRIM,
                        ItemFlag.HIDE_DYE
                );
            } catch (Throwable ignored) {}
        }
    }

    public ItemBuilder(ItemStack itemStack) {
        this.item = itemStack.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (meta != null && name != null) {
            Component comp = ColorUtil.component(name);
            meta.displayName(comp);
        }
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        if (meta != null && lore != null) {
            meta.lore(ColorUtil.componentList(lore));
        }
        return this;
    }

    public ItemBuilder lore(String... lore) {
        if (meta != null && lore != null) {
            List<String> list = new ArrayList<>();
            for (String s : lore) {
                list.add(s);
            }
            meta.lore(ColorUtil.componentList(list));
        }
        return this;
    }

    public ItemBuilder customModelData(int data) {
        if (meta != null && data > 0) {
            meta.setCustomModelData(data);
        }
        return this;
    }

    public ItemBuilder pdc(NamespacedKey key, int value) {
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
        }
        return this;
    }

    public ItemBuilder pdc(NamespacedKey key, String value) {
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
