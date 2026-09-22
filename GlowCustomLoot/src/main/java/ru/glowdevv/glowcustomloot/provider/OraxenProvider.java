package ru.glowdevv.glowcustomloot.provider;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class OraxenProvider implements CustomItemProvider {
    private boolean enabled;

    public OraxenProvider() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("Oraxen");
    }

    public void updateStatus() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("Oraxen");
    }

    @Override
    @NotNull
    public String getProviderId() {
        return "ORAXEN";
    }

    @Override
    public boolean isAvailable() {
        return enabled && Bukkit.getPluginManager().isPluginEnabled("Oraxen");
    }

    @Override
    public boolean canHandle(@NotNull ItemStack item) {
        if (!isAvailable()) return false;
        try {
            Class<?> oraxenItemsClass = Class.forName("io.thillson.oraxen.api.OraxenItems");
            Method getIdByItemMethod = oraxenItemsClass.getMethod("getIdByItem", ItemStack.class);
            Object id = getIdByItemMethod.invoke(null, item);
            return id != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    @Nullable
    public ItemStack buildItem(@NotNull ConfigurationSection section) {
        if (!isAvailable()) return null;
        String itemId = section.getString("item_id");
        if (itemId == null) return null;

        try {
            Class<?> oraxenItemsClass = Class.forName("io.thillson.oraxen.api.OraxenItems");
            Method getItemByIdMethod = oraxenItemsClass.getMethod("getItemById", String.class);
            Object itemModifier = getItemByIdMethod.invoke(null, itemId);
            if (itemModifier != null) {
                Method buildMethod = itemModifier.getClass().getMethod("build");
                return (ItemStack) buildMethod.invoke(itemModifier);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Override
    public void serializeItem(@NotNull ItemStack item, @NotNull ConfigurationSection section) {
        section.set("provider", getProviderId());
        try {
            Class<?> oraxenItemsClass = Class.forName("io.thillson.oraxen.api.OraxenItems");
            Method getIdByItemMethod = oraxenItemsClass.getMethod("getIdByItem", ItemStack.class);
            Object id = getIdByItemMethod.invoke(null, item);
            if (id != null) {
                section.set("item_id", id.toString());
                return;
            }
        } catch (Throwable ignored) {
        }
        section.set("item_id", "unknown");
    }
}
