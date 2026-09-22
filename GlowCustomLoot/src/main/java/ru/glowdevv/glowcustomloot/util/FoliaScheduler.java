package ru.glowdevv.glowcustomloot.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class FoliaScheduler {
    private static final boolean IS_FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
        }
        IS_FOLIA = folia;
    }

    private FoliaScheduler() {
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static void runSync(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().run(plugin, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runAsync(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        if (IS_FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public static void runLater(@NotNull Plugin plugin, @NotNull Runnable runnable, long delayTicks) {
        if (IS_FOLIA) {
            long delayMs = Math.max(1, delayTicks * 50);
            Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {
                runSync(plugin, runnable);
            }, delayMs, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public static void runAtEntity(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable) {
        if (IS_FOLIA) {
            entity.getScheduler().run(plugin, task -> runnable.run(), null);
        } else {
            runSync(plugin, runnable);
        }
    }
}
