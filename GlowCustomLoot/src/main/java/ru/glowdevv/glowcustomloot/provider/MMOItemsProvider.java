package ru.glowdevv.glowcustomloot.provider;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class MMOItemsProvider implements CustomItemProvider {
    private boolean enabled;

    public MMOItemsProvider() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("MMOItems");
    }

    public void updateStatus() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("MMOItems");
    }

    @Override
    @NotNull
    public String getProviderId() {
        return "MMOITEMS";
    }

    @Override
    public boolean isAvailable() {
        return enabled && Bukkit.getPluginManager().isPluginEnabled("MMOItems");
    }

    @Override
    public boolean canHandle(@NotNull ItemStack item) {
        if (!isAvailable()) return false;
        try {
            Class<?> mmoItemsClass = Class.forName("net.Indyuce.mmoitems.MMOItems");
            Method getTypeIdMethod = mmoItemsClass.getMethod("getType", ItemStack.class);
            Object type = getTypeIdMethod.invoke(null, item);
            return type != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    @Nullable
    public ItemStack buildItem(@NotNull ConfigurationSection section) {
        if (!isAvailable()) return null;
        String type = section.getString("mmoitems_type");
        String id = section.getString("item_id");
        if (type == null || id == null) return null;

        try {
            Class<?> mmoItemsClass = Class.forName("net.Indyuce.mmoitems.MMOItems");
            Method getItemMethod = mmoItemsClass.getMethod("getItem", String.class, String.class);
            return (ItemStack) getItemMethod.invoke(null, type, id);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public void serializeItem(@NotNull ItemStack item, @NotNull ConfigurationSection section) {
        section.set("provider", getProviderId());
        try {
            Class<?> mmoItemsClass = Class.forName("net.Indyuce.mmoitems.MMOItems");
            Method getTypeMethod = mmoItemsClass.getMethod("getType", ItemStack.class);
            Object typeObj = getTypeMethod.invoke(null, item);
            Method getIdMethod = mmoItemsClass.getMethod("getID", ItemStack.class);
            Object idObj = getIdMethod.invoke(null, item);
            if (typeObj != null && idObj != null) {
                Method getIdFromType = typeObj.getClass().getMethod("getId");
                section.set("mmoitems_type", getIdFromType.invoke(typeObj).toString());
                section.set("item_id", idObj.toString());
                return;
            }
        } catch (Throwable ignored) {
        }
        section.set("item_id", "unknown");
    }
}
