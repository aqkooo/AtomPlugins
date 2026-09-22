package ru.glowdev.glowsnake.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class SoundUtil {

    private SoundUtil() {}

    public static void playSound(Player player, String soundName, float volume, float pitch) {
        if (player == null || soundName == null || soundName.trim().isEmpty()) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(soundName.trim().toUpperCase(Locale.ROOT));
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Attempt fallback or play by string key if supported
            try {
                player.playSound(player.getLocation(), soundName.toLowerCase(Locale.ROOT), volume, pitch);
            } catch (Exception ignored) {
            }
        }
    }

    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        if (player != null && sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
