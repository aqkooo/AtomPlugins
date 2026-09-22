package ru.atomicsqd.glowtrade.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;

/**
 * Утилита для валидации предметов, защиты от дюпов и создания защищенных элементов GUI.
 */
public final class ItemUtil {

    private static NamespacedKey GUI_ITEM_KEY;

    private ItemUtil() {}

    public static void init(Plugin plugin) {
        GUI_ITEM_KEY = new NamespacedKey(plugin, "gui_item");
    }

    /**
     * Создает защищенный элемент GUI без курсива наковальни и с PDC-меткой.
     */
    public static ItemStack createGuiItem(Material material, int amount, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Убираем курсив наковальни через ColorUtil
            if (displayName != null) {
                meta.setDisplayName(ColorUtil.guiItemName(displayName));
            }
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(ColorUtil.guiItemLore(lore));
            }

            // Помечаем предмет в PersistentDataContainer, чтобы предотвратить любое извлечение
            if (GUI_ITEM_KEY != null) {
                meta.getPersistentDataContainer().set(GUI_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createGuiItem(Material material, String displayName, List<String> lore) {
        return createGuiItem(material, 1, displayName, lore);
    }

    public static ItemStack createGuiItem(Material material, String displayName) {
        return createGuiItem(material, 1, displayName, Collections.emptyList());
    }

    /**
     * Проверяет, является ли предмет служебным элементом интерфейса GUI.
     */
    public static boolean isGuiItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || GUI_ITEM_KEY == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(GUI_ITEM_KEY, PersistentDataType.BYTE);
    }

    /**
     * Проверяет, был ли предмет переименован или модифицирован на наковальне.
     * В ванильном Minecraft любое переименование на наковальне устанавливает стоимость починки (RepairCost > 0).
     */
    public static boolean isAnvilRenamed(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        // Служебные предметы плагина не считаются наковаленными
        if (isGuiItem(item)) {
            return false;
        }

        // Проверка флага стоимости наковальни
        if (meta instanceof Repairable repairable) {
            return repairable.hasRepairCost() && repairable.getRepairCost() > 0;
        }

        return false;
    }

    /**
     * Проверяет, находится ли предмет в черном списке трейда.
     */
    public static boolean isBlacklisted(ItemStack item, List<Material> blockedMaterials, List<Integer> blockedCmd, List<String> blockedTags) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        // 1. Проверка по типу материала
        if (blockedMaterials != null && blockedMaterials.contains(item.getType())) {
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        // 2. Проверка по CustomModelData
        if (blockedCmd != null && !blockedCmd.isEmpty() && meta.hasCustomModelData()) {
            if (blockedCmd.contains(meta.getCustomModelData())) {
                return true;
            }
        }

        // 3. Проверка по строковым тегам в PDC (например, untradeable / nodrop)
        if (blockedTags != null && !blockedTags.isEmpty()) {
            for (String tag : blockedTags) {
                // Проверяем все возможные ключи
                for (NamespacedKey key : meta.getPersistentDataContainer().getKeys()) {
                    if (key.getKey().equalsIgnoreCase(tag)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Безопасно передает предметы игроку в инвентарь или выбрасывает на землю при нехватке места.
     */
    public static void giveOrDropItem(Player player, ItemStack item) {
        if (player == null || item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return;
        }

        // Пытаемся добавить в инвентарь игрока
        var leftover = player.getInventory().addItem(item);

        // Если остались предметы, которые не поместились — безопасно дропаем под ноги
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                if (drop != null && drop.getType() != Material.AIR) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
    }
}
