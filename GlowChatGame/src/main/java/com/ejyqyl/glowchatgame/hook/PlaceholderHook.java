package com.ejyqyl.glowchatgame.hook;

import com.ejyqyl.glowchatgame.GlowChatGame;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages PlaceholderAPI expansion registration and lifecycle.
 *
 * @author ejyqyl, Glowdevv
 */
public final class PlaceholderHook {

    private final GlowChatGame plugin;
    private final List<ChatGameExpansion> registeredExpansions = new ArrayList<>();
    private boolean hooked = false;

    public PlaceholderHook(@NotNull GlowChatGame plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }

        try {
            ChatGameExpansion primary = new ChatGameExpansion(plugin, "glowchatgame");
            primary.register();
            registeredExpansions.add(primary);

            ChatGameExpansion alias = new ChatGameExpansion(plugin, "chatgame");
            alias.register();
            registeredExpansions.add(alias);

            this.hooked = true;
            plugin.getLogger().info("Registered PlaceholderAPI expansion with %glowchatgame_...% and %chatgame_...%!");
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to register PlaceholderAPI expansion: " + t.getMessage());
        }
    }

    public void unregister() {
        for (ChatGameExpansion expansion : registeredExpansions) {
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
