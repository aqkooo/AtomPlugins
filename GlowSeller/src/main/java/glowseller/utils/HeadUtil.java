package glowseller.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;

public final class HeadUtil {
    private static Field profileField;
    private static Constructor<?> gameProfileConstructor;
    private static Constructor<?> propertyConstructor;
    private static Method getPropertiesMethod;
    private static Method putPropertyMethod;

    static {
        try {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
            }

            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            gameProfileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);

            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            propertyConstructor = propertyClass.getConstructor(String.class, String.class);

            getPropertiesMethod = gameProfileClass.getMethod("getProperties");
            Class<?> propertyMapClass = getPropertiesMethod.getReturnType();
            putPropertyMethod = propertyMapClass.getMethod("put", Object.class, Object.class);
        } catch (Exception ignored) {
        }
    }

    private HeadUtil() {}

    public static ItemStack createHeadFromBase64(String base64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (base64 == null || base64.isEmpty()) {
            return head;
        }

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            return head;
        }

        if (profileField != null && gameProfileConstructor != null && propertyConstructor != null) {
            try {
                Object profile = gameProfileConstructor.newInstance(UUID.randomUUID(), "");
                Object property = propertyConstructor.newInstance("textures", base64);
                Object propertyMap = getPropertiesMethod.invoke(profile);
                putPropertyMethod.invoke(propertyMap, "textures", property);

                profileField.set(meta, profile);
                head.setItemMeta(meta);
                return head;
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.FINE, "Failed to apply base64 head texture via profile reflection", e);
            }
        }

        head.setItemMeta(meta);
        return head;
    }

    public static ItemStack createHeadFromPlayer(String playerName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (playerName == null || playerName.isEmpty()) {
            return head;
        }

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            try {
                meta.setOwner(playerName);
            } catch (Exception ignored) {}
            head.setItemMeta(meta);
        }
        return head;
    }
}
