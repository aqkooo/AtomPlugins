package glowseller.models.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Item {
    private final String id;
    private final Material material;
    private final Integer customModelData;
    private final double price;
    private final long points;
    private final String name;
    private final String category;

    public Item(String id, Material material, Integer customModelData, double price, long points, String name) {
        this(id, material, customModelData, price, points, name, "mine");
    }

    public Item(String id, Material material, Integer customModelData, double price, long points, String name, String category) {
        this.id = id;
        this.material = material;
        this.customModelData = customModelData;
        this.price = price;
        this.points = points;
        this.name = name;
        this.category = category != null ? category : "mine";
    }

    public String getCategory() {
        return category;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public double getPrice() {
        return price;
    }

    public long getPoints() {
        return points;
    }

    public String getName() {
        return name;
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getType() != material) {
            return false;
        }

        ItemMeta meta = stack.getItemMeta();
        if (customModelData != null) {
            if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != customModelData) {
                return false;
            }
        } else {
            // Protection: if item has custom model data, do not match as plain item
            if (meta != null && meta.hasCustomModelData()) {
                return false;
            }
        }

        return true;
    }
}
