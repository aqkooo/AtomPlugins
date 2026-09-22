package com.ejyqyl.atomduels.kit;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages kit loading, saving, defaults, and creation from player inventory.
 * Authored by ejyqyl.
 */
public class KitManager {

    private final Plugin plugin;
    private final File kitsFile;
    private final Map<String, Kit> kits = new ConcurrentHashMap<>();

    public KitManager(Plugin plugin) {
        this.plugin = plugin;
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
    }

    public void loadKits() {
        kits.clear();
        if (!kitsFile.exists()) {
            createDefaultKits();
            saveKits();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(kitsFile);
        ConfigurationSection section = config.getConfigurationSection("kits");
        if (section == null) {
            createDefaultKits();
            saveKits();
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection kSec = section.getConfigurationSection(key);
            if (kSec == null) continue;

            Kit kit = new Kit(key);
            kit.setDisplayName(kSec.getString("display-name", key));
            String iconName = kSec.getString("icon", "DIAMOND_SWORD");
            try {
                kit.setIcon(Material.valueOf(iconName));
            } catch (Exception e) {
                kit.setIcon(Material.DIAMOND_SWORD);
            }

            // Items
            ConfigurationSection itemsSec = kSec.getConfigurationSection("items");
            if (itemsSec != null) {
                ItemStack[] items = new ItemStack[36];
                for (String slotStr : itemsSec.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(slotStr);
                        if (slot >= 0 && slot < 36) {
                            items[slot] = itemsSec.getItemStack(slotStr);
                        }
                    } catch (NumberFormatException ignored) {}
                }
                kit.setItems(items);
            }

            kit.setHelmet(kSec.getItemStack("helmet"));
            kit.setChestplate(kSec.getItemStack("chestplate"));
            kit.setLeggings(kSec.getItemStack("leggings"));
            kit.setBoots(kSec.getItemStack("boots"));
            kit.setOffhand(kSec.getItemStack("offhand"));

            kits.put(key.toLowerCase(), kit);
        }
        plugin.getLogger().info("Loaded " + kits.size() + " duel kit(s).");
    }

    public void saveKits() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("kits");

        for (Kit kit : kits.values()) {
            ConfigurationSection kSec = root.createSection(kit.getName());
            kSec.set("display-name", kit.getDisplayName());
            kSec.set("icon", kit.getIcon().name());

            ConfigurationSection itemsSec = kSec.createSection("items");
            ItemStack[] items = kit.getItems();
            if (items != null) {
                for (int i = 0; i < items.length; i++) {
                    if (items[i] != null && !items[i].getType().isAir()) {
                        itemsSec.set(String.valueOf(i), items[i]);
                    }
                }
            }

            if (kit.getHelmet() != null) kSec.set("helmet", kit.getHelmet());
            if (kit.getChestplate() != null) kSec.set("chestplate", kit.getChestplate());
            if (kit.getLeggings() != null) kSec.set("leggings", kit.getLeggings());
            if (kit.getBoots() != null) kSec.set("boots", kit.getBoots());
            if (kit.getOffhand() != null) kSec.set("offhand", kit.getOffhand());
        }

        try {
            config.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save kits.yml", e);
        }
    }

    public Kit getKit(String name) {
        if (name == null) return null;
        return kits.get(name.toLowerCase());
    }

    public Collection<Kit> getKits() {
        return Collections.unmodifiableCollection(kits.values());
    }

    public Kit createKitFromPlayer(String name, Player player) {
        Kit kit = new Kit(name);
        kit.setDisplayName(name);
        kit.setIcon(Material.DIAMOND_SWORD);

        ItemStack[] storage = player.getInventory().getStorageContents();
        ItemStack[] items = new ItemStack[36];
        for (int i = 0; i < 36 && i < storage.length; i++) {
            if (storage[i] != null) items[i] = storage[i].clone();
        }
        kit.setItems(items);

        if (player.getInventory().getHelmet() != null) kit.setHelmet(player.getInventory().getHelmet().clone());
        if (player.getInventory().getChestplate() != null) kit.setChestplate(player.getInventory().getChestplate().clone());
        if (player.getInventory().getLeggings() != null) kit.setLeggings(player.getInventory().getLeggings().clone());
        if (player.getInventory().getBoots() != null) kit.setBoots(player.getInventory().getBoots().clone());
        if (player.getInventory().getItemInOffHand() != null && !player.getInventory().getItemInOffHand().getType().isAir()) {
            kit.setOffhand(player.getInventory().getItemInOffHand().clone());
        }

        kits.put(name.toLowerCase(), kit);
        saveKits();
        return kit;
    }

    public boolean deleteKit(String name) {
        if (kits.remove(name.toLowerCase()) != null) {
            saveKits();
            return true;
        }
        return false;
    }

    private void createDefaultKits() {
        // Classic Kit
        Kit classic = new Kit("Classic");
        classic.setDisplayName("<#00B5FD>Классик");
        classic.setIcon(Material.DIAMOND_SWORD);
        ItemStack cSword = new ItemStack(Material.DIAMOND_SWORD);
        cSword.addEnchantment(Enchantment.SHARPNESS, 3);
        classic.getItems()[0] = cSword;
        classic.getItems()[1] = new ItemStack(Material.BOW);
        classic.getItems()[2] = new ItemStack(Material.GOLDEN_APPLE, 16);
        classic.getItems()[8] = new ItemStack(Material.ARROW, 64);
        classic.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        classic.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        classic.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        classic.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        classic.setOffhand(new ItemStack(Material.SHIELD));
        kits.put(classic.getName().toLowerCase(), classic);

        // Axe Kit
        Kit axe = new Kit("Axe");
        axe.setDisplayName("<#FF5555>Топор и Щит");
        axe.setIcon(Material.DIAMOND_AXE);
        ItemStack aAxe = new ItemStack(Material.DIAMOND_AXE);
        aAxe.addEnchantment(Enchantment.SHARPNESS, 4);
        axe.getItems()[0] = aAxe;
        axe.getItems()[1] = new ItemStack(Material.CROSSBOW);
        axe.getItems()[2] = new ItemStack(Material.GOLDEN_APPLE, 12);
        axe.getItems()[8] = new ItemStack(Material.ARROW, 64);
        axe.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        axe.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        axe.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        axe.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        axe.setOffhand(new ItemStack(Material.SHIELD));
        kits.put(axe.getName().toLowerCase(), axe);

        // Mace Kit (1.21+)
        Kit maceKit = new Kit("Mace");
        maceKit.setDisplayName("<#FFAA00>Булава 1.21");
        maceKit.setIcon(Material.MACE);
        ItemStack maceItem = new ItemStack(Material.MACE);
        try {
            maceItem.addEnchantment(Enchantment.DENSITY, 3);
            maceItem.addEnchantment(Enchantment.WIND_BURST, 1);
        } catch (Throwable ignored) {}
        maceKit.getItems()[0] = maceItem;
        maceKit.getItems()[1] = new ItemStack(Material.WIND_CHARGE, 32);
        maceKit.getItems()[2] = new ItemStack(Material.GOLDEN_APPLE, 16);
        maceKit.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        maceKit.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        maceKit.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        maceKit.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        kits.put(maceKit.getName().toLowerCase(), maceKit);

        // Sumo Kit
        Kit sumo = new Kit("Sumo");
        sumo.setDisplayName("<#FFFF55>Сумо");
        sumo.setIcon(Material.STICK);
        kits.put(sumo.getName().toLowerCase(), sumo);

        // Gapple Kit
        Kit gapple = new Kit("Gapple");
        gapple.setDisplayName("<#FFD700>Золотые яблоки");
        gapple.setIcon(Material.ENCHANTED_GOLDEN_APPLE);
        ItemStack gSword = new ItemStack(Material.DIAMOND_SWORD);
        gSword.addEnchantment(Enchantment.SHARPNESS, 5);
        gapple.getItems()[0] = gSword;
        gapple.getItems()[1] = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 64);
        ItemStack gHelm = new ItemStack(Material.DIAMOND_HELMET);
        gHelm.addEnchantment(Enchantment.PROTECTION, 4);
        ItemStack gChest = new ItemStack(Material.DIAMOND_CHESTPLATE);
        gChest.addEnchantment(Enchantment.PROTECTION, 4);
        ItemStack gLegs = new ItemStack(Material.DIAMOND_LEGGINGS);
        gLegs.addEnchantment(Enchantment.PROTECTION, 4);
        ItemStack gBoots = new ItemStack(Material.DIAMOND_BOOTS);
        gBoots.addEnchantment(Enchantment.PROTECTION, 4);
        gapple.setHelmet(gHelm);
        gapple.setChestplate(gChest);
        gapple.setLeggings(gLegs);
        gapple.setBoots(gBoots);
        kits.put(gapple.getName().toLowerCase(), gapple);
    }
}
