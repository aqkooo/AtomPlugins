package ru.glowdevv.glowcustomloot.provider;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class MythicMobsProvider implements CustomItemProvider {
    private boolean enabled;

    public MythicMobsProvider() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
    }

    public void updateStatus() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
    }

    @Override
    @NotNull
    public String getProviderId() {
        return "MYTHICMOBS";
    }

    @Override
    public boolean isAvailable() {
        return enabled && Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
    }

    @Override
    public boolean canHandle(@NotNull ItemStack item) {
        if (!isAvailable()) return false;
        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method instMethod = mythicBukkitClass.getMethod("get");
            Object inst = instMethod.invoke(null);
            Method getItemManagerMethod = inst.getClass().getMethod("getItemManager");
            Object itemManager = getItemManagerMethod.invoke(inst);
            Method isMythicItemMethod = itemManager.getClass().getMethod("isMythicItem", ItemStack.class);
            return (boolean) isMythicItemMethod.invoke(itemManager, item);
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
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method instMethod = mythicBukkitClass.getMethod("get");
            Object inst = instMethod.invoke(null);
            Method getItemManagerMethod = inst.getClass().getMethod("getItemManager");
            Object itemManager = getItemManagerMethod.invoke(inst);
            Method getItemStackMethod = itemManager.getClass().getMethod("getItemStack", String.class);
            return (ItemStack) getItemStackMethod.invoke(itemManager, itemId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public void serializeItem(@NotNull ItemStack item, @NotNull ConfigurationSection section) {
        section.set("provider", getProviderId());
        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method instMethod = mythicBukkitClass.getMethod("get");
            Object inst = instMethod.invoke(null);
            Method getItemManagerMethod = inst.getClass().getMethod("getItemManager");
            Object itemManager = getItemManagerMethod.invoke(inst);
            Method getMythicTypeMethod = itemManager.getClass().getMethod("getMythicTypeFromItem", ItemStack.class);
            Object type = getMythicTypeMethod.invoke(itemManager, item);
            if (type != null) {
                section.set("item_id", type.toString());
                return;
            }
        } catch (Throwable ignored) {
        }
        section.set("item_id", "unknown");
    }
}
