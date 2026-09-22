package com.ejyqyl.atompvpbot.kit;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages duel kits and player inventory kit creation.
 *
 * @author ejyqyl
 */
public class KitManager {

    private final Plugin plugin;
    private final Map<String, BotKit> kits = new ConcurrentHashMap<>();
    private final File kitsFile;
    private FileConfiguration kitsConfig;

    public KitManager(Plugin plugin) {
        this.plugin = plugin;
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
    }

    public void loadKits() {
        kits.clear();
        if (!kitsFile.exists()) {
            if (plugin.getResource("kits.yml") != null) {
                plugin.saveResource("kits.yml", false);
            }
        }

        this.kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
        ConfigurationSection sec = kitsConfig.getConfigurationSection("kits");
        if (sec == null) {
            createDefaultKits();
            saveKits();
            return;
        }

        for (String key : sec.getKeys(false)) {
            ConfigurationSection kSec = sec.getConfigurationSection(key);
            if (kSec == null) continue;

            BotKit kit = new BotKit(key);
            kit.setDisplayName(kSec.getString("display-name", key));

            String iconName = kSec.getString("icon", "DIAMOND_SWORD");
            Material iconMat = Material.matchMaterial(iconName);
            kit.setIcon(iconMat != null ? iconMat : Material.DIAMOND_SWORD);

            kit.setHelmet(parseItem(kSec.getString("helmet")));
            kit.setChestplate(parseItem(kSec.getString("chestplate")));
            kit.setLeggings(parseItem(kSec.getString("leggings")));
            kit.setBoots(parseItem(kSec.getString("boots")));
            kit.setOffhand(parseItem(kSec.getString("offhand")));

            ConfigurationSection itemsSec = kSec.getConfigurationSection("items");
            if (itemsSec != null) {
                for (String slotStr : itemsSec.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(slotStr);
                        ConfigurationSection itemSec = itemsSec.getConfigurationSection(slotStr);
                        if (itemSec != null) {
                            ItemStack item = parseItemSection(itemSec);
                            if (item != null) kit.getItems().put(slot, item);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            kits.put(key.toLowerCase(), kit);
        }
        plugin.getLogger().info("Loaded " + kits.size() + " kits for AtomPvPbot.");
    }

    public void saveKits() {
        if (kitsConfig == null) kitsConfig = new YamlConfiguration();
        kitsConfig.set("kits", null);

        for (BotKit kit : kits.values()) {
            String path = "kits." + kit.getName();
            kitsConfig.set(path + ".display-name", kit.getDisplayName());
            kitsConfig.set(path + ".icon", kit.getIcon().name());
            if (kit.getHelmet() != null) kitsConfig.set(path + ".helmet", kit.getHelmet().getType().name());
            if (kit.getChestplate() != null) kitsConfig.set(path + ".chestplate", kit.getChestplate().getType().name());
            if (kit.getLeggings() != null) kitsConfig.set(path + ".leggings", kit.getLeggings().getType().name());
            if (kit.getBoots() != null) kitsConfig.set(path + ".boots", kit.getBoots().getType().name());
            if (kit.getOffhand() != null) kitsConfig.set(path + ".offhand", kit.getOffhand().getType().name());

            for (Map.Entry<Integer, ItemStack> entry : kit.getItems().entrySet()) {
                if (entry.getValue() != null) {
                    String itemPath = path + ".items." + entry.getKey();
                    kitsConfig.set(itemPath + ".material", entry.getValue().getType().name());
                    kitsConfig.set(itemPath + ".amount", entry.getValue().getAmount());
                }
            }
        }

        try {
            kitsConfig.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save kits.yml", e);
        }
    }

    public void createKitFromPlayer(String name, Player player) {
        BotKit kit = new BotKit(name);
        kit.setDisplayName("<#00B5FD>" + name);
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            kit.setIcon(player.getInventory().getItemInMainHand().getType());
        }

        if (player.getInventory().getHelmet() != null) kit.setHelmet(player.getInventory().getHelmet().clone());
        if (player.getInventory().getChestplate() != null) kit.setChestplate(player.getInventory().getChestplate().clone());
        if (player.getInventory().getLeggings() != null) kit.setLeggings(player.getInventory().getLeggings().clone());
        if (player.getInventory().getBoots() != null) kit.setBoots(player.getInventory().getBoots().clone());
        if (player.getInventory().getItemInOffHand().getType() != Material.AIR) {
            kit.setOffhand(player.getInventory().getItemInOffHand().clone());
        }

        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && !item.getType().isAir()) {
                kit.getItems().put(i, item.clone());
            }
        }

        kits.put(name.toLowerCase(), kit);
        saveKits();
    }

    public BotKit getKit(String name) {
        if (name == null) return null;
        return kits.get(name.toLowerCase());
    }

    public boolean deleteKit(String name) {
        BotKit removed = kits.remove(name.toLowerCase());
        if (removed != null) {
            saveKits();
            return true;
        }
        return false;
    }

    public Collection<BotKit> getKits() {
        return Collections.unmodifiableCollection(kits.values());
    }

    private void createDefaultKits() {
        BotKit classic = new BotKit("Classic");
        classic.setDisplayName("<#00B5FD>Классик");
        classic.setIcon(Material.DIAMOND_SWORD);
        classic.getItems().put(0, new ItemStack(Material.DIAMOND_SWORD));
        classic.getItems().put(1, new ItemStack(Material.BOW));
        classic.getItems().put(2, new ItemStack(Material.GOLDEN_APPLE, 16));
        classic.getItems().put(8, new ItemStack(Material.ARROW, 64));
        classic.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        classic.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        classic.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        classic.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        classic.setOffhand(new ItemStack(Material.SHIELD));
        kits.put(classic.getName().toLowerCase(), classic);

        BotKit maceKit = new BotKit("Mace");
        maceKit.setDisplayName("<#FFAA00>Булава 1.21");
        maceKit.setIcon(Material.MACE);
        maceKit.getItems().put(0, new ItemStack(Material.MACE));
        maceKit.getItems().put(1, new ItemStack(Material.WIND_CHARGE, 32));
        maceKit.getItems().put(2, new ItemStack(Material.GOLDEN_APPLE, 16));
        maceKit.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        maceKit.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        maceKit.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        maceKit.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        kits.put(maceKit.getName().toLowerCase(), maceKit);
    }

    private ItemStack parseItem(String matName) {
        if (matName == null || matName.isEmpty()) return null;
        Material m = Material.matchMaterial(matName);
        return m != null ? new ItemStack(m) : null;
    }

    private ItemStack parseItemSection(ConfigurationSection sec) {
        String matName = sec.getString("material");
        if (matName == null) return null;
        Material m = Material.matchMaterial(matName);
        if (m == null) return null;

        int amount = sec.getInt("amount", 1);
        ItemStack item = new ItemStack(m, amount);

        ConfigurationSection enchants = sec.getConfigurationSection("enchantments");
        if (enchants != null) {
            for (String enchKey : enchants.getKeys(false)) {
                Enchantment ench = Enchantment.getByName(enchKey.toUpperCase());
                if (ench != null) {
                    item.addUnsafeEnchantment(ench, enchants.getInt(enchKey, 1));
                }
            }
        }
        return item;
    }
}
