package com.ejyqyl.atomduels.hook;

import com.ejyqyl.atomduels.AtomDuels;
import org.bukkit.Bukkit;

/**
 * Manages PlaceholderAPI registration for AtomDuels.
 * Authored by ejyqyl (https://github.com/aqkooo).
 */
public class PlaceholderHook {

    private final AtomDuels plugin;
    private boolean enabled = false;

    public PlaceholderHook(AtomDuels plugin) {
        this.plugin = plugin;
        register();
    }

    private void register() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DuelsExpansion(plugin).register();
            this.enabled = true;
            plugin.getLogger().info("PlaceholderAPI expansion registered for %atomduels%!");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
