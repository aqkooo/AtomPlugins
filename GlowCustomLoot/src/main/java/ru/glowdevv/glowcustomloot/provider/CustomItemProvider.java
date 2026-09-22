package ru.glowdevv.glowcustomloot.provider;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CustomItemProvider {

    @NotNull
    String getProviderId();

    boolean isAvailable();

    boolean canHandle(@NotNull ItemStack item);

    @Nullable
    ItemStack buildItem(@NotNull ConfigurationSection section);

    void serializeItem(@NotNull ItemStack item, @NotNull ConfigurationSection targetSection);
}
