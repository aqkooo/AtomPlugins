package ru.glowdevv.glowcustomloot.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.glowdevv.glowcustomloot.GlowCustomLootPlugin;
import ru.glowdevv.glowcustomloot.model.DimensionType;
import ru.glowdevv.glowcustomloot.util.TextUtil;

import java.util.List;

public class DimensionMenu {
    private final GlowCustomLootPlugin plugin;

    public DimensionMenu(@NotNull GlowCustomLootPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(@NotNull Player player) {
        ConfigurationSection menuSec = plugin.getConfigManager().getMenus().getConfigurationSection("main_menu");
        int size = menuSec != null ? menuSec.getInt("size", 27) : 27;
        if (size % 9 != 0 || size < 9 || size > 54) size = 27;

        String rawTitle = menuSec != null ? menuSec.getString("title", "Выбор измерения") : "Выбор измерения";
        Component titleComp = TextUtil.parse(rawTitle, false);

        MenuHolder holder = new MenuHolder("DIMENSION_MENU");
        Inventory inv = Bukkit.createInventory(holder, size, titleComp);
        holder.setInventory(inv);

        // Fill background
        if (menuSec != null && menuSec.isConfigurationSection("filler")) {
            String fillerMatName = menuSec.getString("filler.material", "BLACK_STAINED_GLASS_PANE");
            Material fillerMat = Material.matchMaterial(fillerMatName);
            if (fillerMat == null) fillerMat = Material.BLACK_STAINED_GLASS_PANE;
            String fillerName = menuSec.getString("filler.name", " ");
            ItemStack fillerItem = createSimpleItem(fillerMat, fillerName, null);

            for (int i = 0; i < size; i++) {
                inv.setItem(i, fillerItem);
            }
        }

        // Setup dimension items
        setupDimensionItem(inv, holder, player, menuSec, "items.overworld", DimensionType.OVERWORLD);
        setupDimensionItem(inv, holder, player, menuSec, "items.nether", DimensionType.NETHER);
        setupDimensionItem(inv, holder, player, menuSec, "items.the_end", DimensionType.THE_END);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    private void setupDimensionItem(@NotNull Inventory inv, @NotNull MenuHolder holder,
                                    @NotNull Player player, @Nullable ConfigurationSection menuSec,
                                    @NotNull String path, @NotNull DimensionType dimension) {
        int slot = dimension.getDefaultSlot();
        Material mat = dimension.getDefaultIcon();
        String name = "<!italic><gold><b>" + dimension.getDisplayName() + "</b></gold>";
        List<String> lore = null;

        if (menuSec != null && menuSec.isConfigurationSection(path)) {
            ConfigurationSection sec = menuSec.getConfigurationSection(path);
            slot = sec.getInt("slot", slot);
            String matName = sec.getString("material");
            if (matName != null) {
                Material parsed = Material.matchMaterial(matName);
                if (parsed != null) mat = parsed;
            }
            if (sec.isString("name")) name = sec.getString("name");
            if (sec.isList("lore")) lore = sec.getStringList("lore");
        }

        ItemStack item = createSimpleItem(mat, name, lore);
        if (slot >= 0 && slot < inv.getSize()) {
            inv.setItem(slot, item);
            holder.setHandler(slot, event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new StructureMenu(plugin, dimension, 1).open(player);
            });
        }
    }

    private ItemStack createSimpleItem(@NotNull Material mat, @NotNull String name, @Nullable List<String> lore) {
        ItemStack item = new ItemStack(mat);
        item.editMeta(meta -> {
            meta.displayName(TextUtil.parse(name, true));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(TextUtil.parseLore(lore));
            }
        });
        return item;
    }
}
