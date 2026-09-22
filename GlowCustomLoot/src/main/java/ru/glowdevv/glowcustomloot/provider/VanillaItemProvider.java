package ru.glowdevv.glowcustomloot.provider;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.glowdevv.glowcustomloot.util.TextUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VanillaItemProvider implements CustomItemProvider {

    @Override
    @NotNull
    public String getProviderId() {
        return "VANILLA";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean canHandle(@NotNull ItemStack item) {
        return true; // Fallback for all items
    }

    @Override
    @Nullable
    public ItemStack buildItem(@NotNull ConfigurationSection section) {
        // If complex Bukkit serialization exists, load it first
        if (section.isConfigurationSection("bukkit_data")) {
            ConfigurationSection bukkitSec = section.getConfigurationSection("bukkit_data");
            if (bukkitSec != null) {
                Map<String, Object> values = bukkitSec.getValues(true);
                try {
                    ItemStack deserialized = ItemStack.deserialize(values);
                    applyOverrides(deserialized, section);
                    return deserialized;
                } catch (Exception ignored) {
                }
            }
        }

        String matName = section.getString("material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) {
            mat = Material.STONE;
        }

        ItemStack item = new ItemStack(mat);
        applyOverrides(item, section);
        return item;
    }

    private void applyOverrides(@NotNull ItemStack item, @NotNull ConfigurationSection section) {
        item.editMeta(meta -> {
            if (section.isString("name")) {
                Component nameComp = TextUtil.parse(section.getString("name"), true);
                meta.displayName(nameComp);
            }

            if (section.isList("lore")) {
                List<String> rawLore = section.getStringList("lore");
                meta.lore(TextUtil.parseLore(rawLore));
            }

            if (section.isInt("custom_model_data")) {
                meta.setCustomModelData(section.getInt("custom_model_data"));
            }

            if (section.isBoolean("unbreakable")) {
                meta.setUnbreakable(section.getBoolean("unbreakable"));
            }

            if (meta instanceof Damageable damageable && section.isInt("damage")) {
                damageable.setDamage(section.getInt("damage"));
            }
        });

        if (section.isConfigurationSection("enchantments")) {
            ConfigurationSection enchSec = section.getConfigurationSection("enchantments");
            if (enchSec != null) {
                for (String key : enchSec.getKeys(false)) {
                    Enchantment ench = resolveEnchantment(key);
                    if (ench != null) {
                        int level = enchSec.getInt(key, 1);
                        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta) {
                            item.editMeta(org.bukkit.inventory.meta.EnchantmentStorageMeta.class, storageMeta -> {
                                storageMeta.addStoredEnchant(ench, level, true);
                            });
                        } else {
                            item.addUnsafeEnchantment(ench, level);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void serializeItem(@NotNull ItemStack item, @NotNull ConfigurationSection section) {
        section.set("provider", getProviderId());
        section.set("material", item.getType().name());

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                // Serialized as MiniMessage representation
                Component name = meta.displayName();
                if (name != null) {
                    section.set("name", TextUtil.convertToMiniMessage(TextUtil.translateHexToSection(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(name))));
                }
            }

            if (meta.hasLore()) {
                List<Component> lore = meta.lore();
                if (lore != null && !lore.isEmpty()) {
                    List<String> rawLore = new ArrayList<>();
                    for (Component line : lore) {
                        rawLore.add(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line));
                    }
                    section.set("lore", rawLore);
                }
            }

            if (meta.hasCustomModelData()) {
                section.set("custom_model_data", meta.getCustomModelData());
            }

            if (meta.isUnbreakable()) {
                section.set("unbreakable", true);
            }

            if (meta instanceof Damageable damageable && damageable.hasDamage()) {
                section.set("damage", damageable.getDamage());
            }
        }

        if (meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta storageMeta && storageMeta.hasStoredEnchants()) {
            Map<String, Integer> enchants = new HashMap<>();
            for (Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                enchants.put(entry.getKey().getKey().getKey().toUpperCase(), entry.getValue());
            }
            section.set("enchantments", enchants);
        } else if (!item.getEnchantments().isEmpty()) {
            Map<String, Integer> enchants = new HashMap<>();
            for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                enchants.put(entry.getKey().getKey().getKey().toUpperCase(), entry.getValue());
            }
            section.set("enchantments", enchants);
        }

        // Also save full Bukkit serialization to guarantee 100% preservation of all Paper components, PDC, potions, trims, etc.
        ConfigurationSection bukkitSec = section.createSection("bukkit_data");
        for (Map.Entry<String, Object> entry : item.serialize().entrySet()) {
            bukkitSec.set(entry.getKey(), entry.getValue());
        }
    }

    @Nullable
    private Enchantment resolveEnchantment(@NotNull String key) {
        String normalized = key.toLowerCase().replace("minecraft:", "");
        try {
            NamespacedKey namespacedKey = NamespacedKey.minecraft(normalized);
            Enchantment ench = Registry.ENCHANTMENT.get(namespacedKey);
            if (ench != null) return ench;
        } catch (Exception ignored) {
        }
        return Enchantment.getByName(key.toUpperCase());
    }
}
