package com.ejyqyl.glowcmd.hook;

import com.ejyqyl.glowcmd.GlowCMD;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages registration and lifecycle of PlaceholderAPI expansions for GlowCMD.
 *
 * @author ejyqyl
 */
public final class PlaceholderHook {

    private final GlowCMD plugin;
    private final List<GlowCMDExpansion> registeredExpansions = new ArrayList<>();
    private boolean hooked = false;

    public PlaceholderHook(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }

        try {
            // Register primary %glowcmd_...%
            GlowCMDExpansion primary = new GlowCMDExpansion(plugin, "glowcmd");
            primary.register();
            registeredExpansions.add(primary);

            // Register short %glow_...%
            GlowCMDExpansion shortExp = new GlowCMDExpansion(plugin, "glow");
            shortExp.register();
            registeredExpansions.add(shortExp);

            // Register %essentials_...% fallback if not registered by an external jar
            if (!PlaceholderAPI.isRegistered("essentials")) {
                GlowCMDExpansion essentialsCompat = new GlowCMDExpansion(plugin, "essentials");
                essentialsCompat.register();
                registeredExpansions.add(essentialsCompat);
                plugin.getLogger().info("Registered %essentials_...% compatibility expansion for legacy configurations.");
            }

            this.hooked = true;
            plugin.getLogger().info("Successfully hooked into PlaceholderAPI with %glowcmd_...% and %glow_...%!");
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to register PlaceholderAPI expansions: " + t.getMessage());
        }
    }

    public void unregister() {
        for (GlowCMDExpansion expansion : registeredExpansions) {
            try {
                expansion.unregister();
            } catch (Throwable ignored) {
            }
        }
        registeredExpansions.clear();
        this.hooked = false;
    }

    public boolean isHooked() {
        return hooked;
    }
}
