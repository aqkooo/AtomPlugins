package glowseller.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ModelDataUtil {
    private ModelDataUtil() {}

    public static void setCustomModelData(ItemStack item, Integer data) {
        if (item == null || data == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(data);
            item.setItemMeta(meta);
        }
    }

    public static Integer getCustomModelData(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasCustomModelData()) {
            return meta.getCustomModelData();
        }
        return null;
    }
}
