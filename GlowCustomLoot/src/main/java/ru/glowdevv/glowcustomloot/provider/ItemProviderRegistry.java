package ru.glowdevv.glowcustomloot.provider;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemProviderRegistry {
    private final List<CustomItemProvider> providers = new ArrayList<>();
    private final Map<String, CustomItemProvider> byId = new HashMap<>();
    private final VanillaItemProvider fallbackVanilla = new VanillaItemProvider();

    public ItemProviderRegistry() {
        registerDefaults();
    }

    public ItemProviderRegistry(@Nullable org.bukkit.plugin.java.JavaPlugin plugin) {
        this();
    }

    public void registerDefaults() {
        if (providers.isEmpty()) {
            register(new OraxenProvider());
            register(new ItemsAdderProvider());
            register(new MythicMobsProvider());
            register(new MMOItemsProvider());
            register(fallbackVanilla);
        }
    }

    @NotNull
    public String detectProvider(@NotNull ItemStack item) {
        return findProviderFor(item).getProviderId();
    }

    public void register(@NotNull CustomItemProvider provider) {
        providers.add(provider);
        byId.put(provider.getProviderId().toUpperCase(), provider);
    }

    public void reloadIntegrations() {
        for (CustomItemProvider provider : providers) {
            if (provider instanceof ItemsAdderProvider ia) ia.updateStatus();
            if (provider instanceof OraxenProvider ox) ox.updateStatus();
            if (provider instanceof MythicMobsProvider mm) mm.updateStatus();
            if (provider instanceof MMOItemsProvider mmo) mmo.updateStatus();
        }
    }

    @NotNull
    public CustomItemProvider findProviderFor(@NotNull ItemStack item) {
        for (CustomItemProvider provider : providers) {
            if (provider == fallbackVanilla) continue; // check hooks first
            if (provider.isAvailable() && provider.canHandle(item)) {
                return provider;
            }
        }
        return fallbackVanilla;
    }

    @NotNull
    public CustomItemProvider getProvider(@Nullable String id) {
        if (id == null) return fallbackVanilla;
        CustomItemProvider provider = byId.get(id.toUpperCase());
        return provider != null ? provider : fallbackVanilla;
    }

    @NotNull
    public ItemStack buildItem(@NotNull ConfigurationSection section) {
        String providerId = section.getString("provider", "VANILLA");
        CustomItemProvider provider = getProvider(providerId);
        ItemStack item = null;
        if (provider.isAvailable()) {
            item = provider.buildItem(section);
        }

        if (item == null) {
            item = fallbackVanilla.buildItem(section);
        }

        if (item == null) {
            item = new ItemStack(Material.STONE);
        }
        return item;
    }

    public void serializeItem(@NotNull ItemStack item, @NotNull ConfigurationSection section) {
        CustomItemProvider provider = findProviderFor(item);
        provider.serializeItem(item, section);
    }
}
