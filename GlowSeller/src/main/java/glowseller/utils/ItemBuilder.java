package glowseller.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material != null ? material : Material.STONE);
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item != null ? item.clone() : new ItemStack(Material.STONE);
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemBuilder name(String name) {
        if (meta != null && name != null) {
            meta.setDisplayName(Colorizer.colorize(name));
        }
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        if (meta != null && lines != null) {
            meta.setLore(Colorizer.colorize(lines));
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    public ItemBuilder addLore(String line) {
        if (meta != null && line != null) {
            List<String> current = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            if (current == null) current = new ArrayList<>();
            current.add(Colorizer.colorize(line));
            meta.setLore(current);
        }
        return this;
    }

    public ItemBuilder customModelData(Integer data) {
        if (meta != null && data != null) {
            meta.setCustomModelData(data);
        }
        return this;
    }

    public ItemBuilder glow() {
        if (meta != null) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        if (meta != null && flags != null) {
            meta.addItemFlags(flags);
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
