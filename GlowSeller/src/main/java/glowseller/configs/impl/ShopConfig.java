package glowseller.configs.impl;

import glowseller.Main;
import glowseller.configs.CustomConfig;
import glowseller.models.ShopItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.logging.Level;

public class ShopConfig extends CustomConfig {
    public static class Category {
        private final String id;
        private final String name;
        private final int slot;
        private final Material material;
        private final String baseHead;
        private final List<String> lore;
        private final Map<String, ShopItem> items = new LinkedHashMap<>();

        public Category(String id, String name, int slot, Material material, String baseHead, List<String> lore) {
            this.id = id;
            this.name = name;
            this.slot = slot;
            this.material = material;
            this.baseHead = baseHead;
            this.lore = lore != null ? lore : new ArrayList<>();
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getSlot() {
            return slot;
        }

        public Material getMaterial() {
            return material;
        }

        public String getBaseHead() {
            return baseHead;
        }

        public List<String> getLore() {
            return lore;
        }

        public Map<String, ShopItem> getItems() {
            return Collections.unmodifiableMap(items);
        }

        public void addItem(ShopItem item) {
            items.put(item.getKey(), item);
        }
    }

    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<String, ShopItem> allItems = new HashMap<>();
    private Material decorateMaterial = Material.GRAY_STAINED_GLASS_PANE;
    private final List<Integer> decorateSlots = new ArrayList<>();

    public ShopConfig(Main plugin) {
        super(plugin, "shop.yml");
        reload();
    }

    @Override
    public void parse() {
        categories.clear();
        allItems.clear();
        decorateSlots.clear();

        // 1. Decorate section
        ConfigurationSection decSec = config.getConfigurationSection("decorate");
        if (decSec != null) {
            String matName = decSec.getString("material", "GRAY_STAINED_GLASS_PANE");
            Material mat = Material.matchMaterial(matName);
            if (mat != null) {
                decorateMaterial = mat;
            }
            List<String> slotSpecs = decSec.getStringList("slots");
            for (String spec : slotSpecs) {
                parseSlots(spec, decorateSlots);
            }
        }

        // 2. Categories
        ConfigurationSection catSec = config.getConfigurationSection("categories");
        if (catSec != null) {
            for (String catKey : catSec.getKeys(false)) {
                ConfigurationSection sec = catSec.getConfigurationSection(catKey);
                if (sec == null) continue;

                String name = sec.getString("name", catKey);
                int slot = sec.getInt("slot", 10 + categories.size() * 2);
                String rawMat = sec.getString("material", "CHEST");
                Material mat = Material.matchMaterial(rawMat);
                String baseHead = null;
                if (rawMat.startsWith("basehead-")) {
                    mat = Material.PLAYER_HEAD;
                    baseHead = rawMat.substring("basehead-".length());
                } else if (mat == null) {
                    mat = Material.CHEST;
                }
                List<String> lore = sec.getStringList("lore");

                Category category = new Category(catKey, name, slot, mat, baseHead, lore);

                ConfigurationSection itemsSec = sec.getConfigurationSection("items");
                if (itemsSec != null) {
                    for (String itemKey : itemsSec.getKeys(false)) {
                        ConfigurationSection is = itemsSec.getConfigurationSection(itemKey);
                        if (is == null) continue;

                        ShopItem item = parseShopItem(itemKey, catKey, is);
                        if (item != null) {
                            category.addItem(item);
                            allItems.put(itemKey, item);
                        }
                    }
                }

                categories.put(catKey, category);
            }
        }

        // 3. Direct items section (if configured directly without categories)
        ConfigurationSection directItemsSec = config.getConfigurationSection("items");
        if (directItemsSec != null) {
            Category defaultCat = categories.computeIfAbsent("default", k -> new Category("default", "Магазин", 0, Material.CHEST, null, new ArrayList<>()));
            for (String itemKey : directItemsSec.getKeys(false)) {
                ConfigurationSection is = directItemsSec.getConfigurationSection(itemKey);
                if (is == null) continue;
                ShopItem item = parseShopItem(itemKey, "default", is);
                if (item != null) {
                    defaultCat.addItem(item);
                    allItems.put(itemKey, item);
                }
            }
        }
    }

    private ShopItem parseShopItem(String itemKey, String catKey, ConfigurationSection is) {
        ShopItem item = new ShopItem(itemKey, catKey);
        String itemMatStr = is.getString("material", "STONE");
        if (itemMatStr.startsWith("basehead-")) {
            item.setMaterial(Material.PLAYER_HEAD);
            item.setBaseHeadTexture(itemMatStr.substring("basehead-".length()));
        } else {
            Material im = Material.matchMaterial(itemMatStr);
            item.setMaterial(im != null ? im : Material.STONE);
        }

        item.setName(is.getString("name", is.getString("display_name", itemKey)));
        item.setLore(is.getStringList("lore"));
        item.setSlot(is.getInt("slot", 0));
        item.setPrice(is.getLong("price", 0L));

        String typeStr = is.getString("type", "ITEM").toUpperCase();
        try {
            item.setType(ShopItem.Type.valueOf(typeStr));
        } catch (IllegalArgumentException e) {
            item.setType(ShopItem.Type.ITEM);
        }

        item.setMultiplier(is.getDouble("multiplier", 1.0));
        item.setDurationSeconds(is.getLong("duration", 0L));
        item.setGiveAmount(is.getInt("give-amount", 1));
        item.setCommands(is.getStringList("commands"));
        if (is.contains("custom_model_data")) {
            item.setCustomModelData(is.getInt("custom_model_data"));
        }
        item.setPermission(is.getString("permission", null));
        item.setLimit(is.getString("limit", "unlimited"));
        return item;
    }

    private void parseSlots(String spec, List<Integer> target) {
        if (spec.contains("-")) {
            String[] parts = spec.split("-");
            try {
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());
                for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
                    target.add(i);
                }
            } catch (NumberFormatException ignored) {}
        } else {
            try {
                target.add(Integer.parseInt(spec.trim()));
            } catch (NumberFormatException ignored) {}
        }
    }

    public Map<String, Category> getCategories() {
        return Collections.unmodifiableMap(categories);
    }

    public Category getCategory(String id) {
        return categories.get(id);
    }

    public ShopItem getItem(String key) {
        return allItems.get(key);
    }

    public Collection<ShopItem> getAllItems() {
        return Collections.unmodifiableCollection(allItems.values());
    }

    public Material getDecorateMaterial() {
        return decorateMaterial;
    }

    public List<Integer> getDecorateSlots() {
        return Collections.unmodifiableList(decorateSlots);
    }
}
