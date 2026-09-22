package com.ejyqyl.atompvpbot.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent ItemStack builder with hideFlags support for clean GUIs.
 *
 * @author ejyqyl
 */
public final class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(@NotNull Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(@NotNull ItemStack source) {
        this.item = source.clone();
        this.meta = item.getItemMeta();
    }

    @NotNull
    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    @NotNull
    public ItemBuilder name(@Nullable String name) {
        if (meta != null && name != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
        }
        return this;
    }

    @NotNull
    public ItemBuilder lore(@NotNull List<String> loreLines) {
        if (meta != null) {
            List<String> colored = new ArrayList<>();
            for (String line : loreLines) {
                colored.add(ColorUtil.colorize(line));
            }
            meta.setLore(colored);
        }
        return this;
    }

    @NotNull
    public ItemBuilder lore(@NotNull String... lines) {
        return lore(List.of(lines));
    }

    @NotNull
    public ItemBuilder enchant(@NotNull Enchantment enchantment, int level) {
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    @NotNull
    public ItemBuilder flags(@NotNull ItemFlag... flags) {
        if (meta != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }

    @NotNull
    public ItemBuilder hideFlags() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    @NotNull
    public ItemBuilder pdc(@NotNull NamespacedKey key, @NotNull String value) {
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        }
        return this;
    }

    @NotNull
    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
