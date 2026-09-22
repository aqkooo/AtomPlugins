package ru.atomicsqd.glowtrade.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;

/**
 * Кросс-версионная утилита для безопасного воспроизведения звуков (1.16.5 – 1.21+).
 * Предотвращает краши при различиях в именах enum Sound между версиями Minecraft.
 */
public final class SoundUtil {

    private static final Map<String, Sound> SOUND_CACHE = new HashMap<>();

    private SoundUtil() {}

    /**
     * Безопасно воспроизводит звук игроку.
     *
     * @param player    Игрок
     * @param soundName Имя звука из конфигурации
     * @param volume    Громкость
     * @param pitch     Высота тона
     */
    public static void playSound(Player player, String soundName, float volume, float pitch) {
        if (player == null || !player.isOnline() || soundName == null || soundName.isEmpty() || soundName.equalsIgnoreCase("NONE")) {
            return;
        }

        Sound sound = resolveSound(soundName);
        if (sound != null) {
            try {
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception ignored) {
                // Игнорируем возможные внутренние ошибки звукового движка сервера
            }
        }
    }

    /**
     * Находит подходящий Sound по названию или фоллбекам.
     */
    public static Sound resolveSound(String soundName) {
        String normalized = soundName.toUpperCase().trim();
        if (SOUND_CACHE.containsKey(normalized)) {
            return SOUND_CACHE.get(normalized);
        }

        Sound sound = null;
        try {
            sound = Sound.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            // Попытка найти аналоги среди разных версий
            sound = findFallback(normalized);
        }

        SOUND_CACHE.put(normalized, sound);
        return sound;
    }

    private static Sound findFallback(String name) {
        // Таблица версионных соответствий
        if (name.contains("EXPERIENCE_ORB") || name.contains("ORB_PICKUP")) {
            return trySound("ENTITY_EXPERIENCE_ORB_PICKUP", "ENTITY_PLAYER_LEVELUP");
        }
        if (name.contains("NOTE_BLOCK") || name.contains("PLING")) {
            return trySound("BLOCK_NOTE_BLOCK_PLING", "BLOCK_NOTE_PLING");
        }
        if (name.contains("NOTE_BLOCK_HAT") || name.contains("HAT")) {
            return trySound("BLOCK_NOTE_BLOCK_HAT", "BLOCK_NOTE_HAT", "UI_BUTTON_CLICK");
        }
        if (name.contains("NOTE_BLOCK_BASS") || name.contains("BASS")) {
            return trySound("BLOCK_NOTE_BLOCK_BASS", "BLOCK_NOTE_BASS", "ENTITY_VILLAGER_NO");
        }
        if (name.contains("BUTTON_CLICK") || name.contains("CLICK")) {
            return trySound("UI_BUTTON_CLICK", "CLICK");
        }
        if (name.contains("LEVELUP")) {
            return trySound("ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
        }
        if (name.contains("VILLAGER_NO")) {
            return trySound("ENTITY_VILLAGER_NO", "VILLAGER_NO");
        }
        return null;
    }

    private static Sound trySound(String... candidates) {
        for (String candidate : candidates) {
            try {
                return Sound.valueOf(candidate);
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }
}
