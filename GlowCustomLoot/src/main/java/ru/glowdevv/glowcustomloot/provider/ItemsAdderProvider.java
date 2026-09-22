package ru.glowdevv.glowcustomloot.provider;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class ItemsAdderProvider implements CustomItemProvider {
    private boolean enabled;

    public ItemsAdderProvider() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");
    }

    public void updateStatus() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");
    }

    @Override
    @NotNull
    public String getProviderId() {
        return "ITEMSADDER";
    }

    @Override
    public boolean isAvailable() {
        return enabled && Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");
    }

    @Override
    public boolean canHandle(@NotNull ItemStack item) {
        if (!isAvailable()) return false;
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method byItemStackMethod = customStackClass.getMethod("byItemStack", ItemStack.class);
            Object customStack = byItemStackMethod.invoke(null, item);
            return customStack != null;
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
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method getInstanceMethod = customStackClass.getMethod("getInstance", String.class);
            Object customStack = getInstanceMethod.invoke(null, itemId);
            if (customStack != null) {
                Method getItemStackMethod = customStackClass.getMethod("getItemStack");
                return (ItemStack) getItemStackMethod.invoke(customStack);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Override
    public void serializeItem(@NotNull ItemStack item, @NotNull ConfigurationSection section) {
        section.set("provider", getProviderId());
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method byItemStackMethod = customStackClass.getMethod("byItemStack", ItemStack.class);
            Object customStack = byItemStackMethod.invoke(null, item);
            if (customStack != null) {
                Method getNamespacedIdMethod = customStackClass.getMethod("getNamespacedID");
                String id = (String) getNamespacedIdMethod.invoke(customStack);
                section.set("item_id", id);
                return;
            }
        } catch (Throwable ignored) {
        }
        section.set("item_id", "unknown");
    }
}
