package glowseller.configs.impl;

import glowseller.Main;
import glowseller.configs.CustomConfig;
import glowseller.models.item.Item;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ItemsConfig extends CustomConfig {
    private final Map<String, Item> items = new HashMap<>();

    public ItemsConfig(Main plugin) {
        super(plugin, "items.yml");
        reload();
    }

    @Override
    public void parse() {
        items.clear();
        ConfigurationSection section = config.getConfigurationSection("items");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSec = section.getConfigurationSection(key);
            if (itemSec == null) continue;

            String matName = itemSec.getString("material", key);
            Material material = Material.matchMaterial(matName);
            if (material == null) {
                plugin.getLogger().log(Level.WARNING, "Unknown material in items.yml: " + matName + " (key: " + key + ")");
                continue;
            }

            double price = itemSec.getDouble("price", 0.0);
            long points = itemSec.getLong("points", 0L);
            String name = itemSec.getString("name", null);
            String category = itemSec.getString("category", "mine");
            Integer customModelData = itemSec.contains("custom_model_data") ? itemSec.getInt("custom_model_data") : null;

            Item item = new Item(key, material, customModelData, price, points, name, category);
            items.put(key, item);
        }
    }

    public Map<String, Item> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public Item getItem(String key) {
        return items.get(key);
    }
}
