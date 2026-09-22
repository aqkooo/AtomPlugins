package ru.glowdevv.glowcustomloot.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.glowdevv.glowcustomloot.GlowCustomLootPlugin;
import ru.glowdevv.glowcustomloot.model.DimensionType;
import ru.glowdevv.glowcustomloot.model.StructureLootTable;
import ru.glowdevv.glowcustomloot.util.TextUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StructureMenu {
    private final GlowCustomLootPlugin plugin;
    private final DimensionType dimension;
    private final int page;

    public StructureMenu(@NotNull GlowCustomLootPlugin plugin, @NotNull DimensionType dimension, int page) {
        this.plugin = plugin;
        this.dimension = dimension;
        this.page = Math.max(1, page);
    }

    public void open(@NotNull Player player) {
        ConfigurationSection menuSec = plugin.getConfigManager().getMenus().getConfigurationSection("structure_menu");
        int size = menuSec != null ? menuSec.getInt("size", 54) : 54;
        if (size % 9 != 0 || size < 9 || size > 54) size = 54;

        String rawTitle = menuSec != null ? menuSec.getString("title", "Данжи: {dimension}") : "Данжи: {dimension}";
        rawTitle = rawTitle.replace("{dimension}", dimension.getDisplayName());
        Component titleComp = TextUtil.parse(rawTitle, false);

        MenuHolder holder = new MenuHolder("STRUCTURE_MENU_" + dimension.name());
        Inventory inv = Bukkit.createInventory(holder, size, titleComp);
        holder.setInventory(inv);

        // Fill background
        if (menuSec != null && menuSec.isConfigurationSection("filler")) {
            String fillerMatName = menuSec.getString("filler.material", "GRAY_STAINED_GLASS_PANE");
            Material fillerMat = Material.matchMaterial(fillerMatName);
            if (fillerMat == null) fillerMat = Material.GRAY_STAINED_GLASS_PANE;
            String fillerName = menuSec.getString("filler.name", " ");
            ItemStack fillerItem = createSimpleItem(fillerMat, fillerName, null);

            for (int i = 0; i < size; i++) {
                inv.setItem(i, fillerItem);
            }
        }

        // Slots for structures
        List<Integer> structureSlots = new ArrayList<>();
        if (menuSec != null && menuSec.isList("structure_slots")) {
            structureSlots = menuSec.getIntegerList("structure_slots");
        }
        if (structureSlots.isEmpty()) {
            for (int r = 1; r <= 3; r++) {
                for (int c = 1; c <= 7; c++) {
                    structureSlots.add(r * 9 + c);
                }
            }
        }

        List<StructureLootTable> tables = plugin.getLootTableManager().getTablesForDimension(dimension);
        tables.sort(Comparator.comparing(StructureLootTable::getId));

        int slotsPerPage = structureSlots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) tables.size() / (double) slotsPerPage));
        int currentPage = Math.clamp(this.page, 1, totalPages);

        int startIndex = (currentPage - 1) * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, tables.size());

        // Setup structure items
        for (int i = startIndex; i < endIndex; i++) {
            StructureLootTable table = tables.get(i);
            int slot = structureSlots.get(i - startIndex);

            ItemStack item = buildStructureItem(menuSec, table);
            inv.setItem(slot, item);

            holder.setHandler(slot, event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new LootEditorMenu(plugin, table, dimension, currentPage, 1).open(player);
            });
        }

        // Setup back button
        if (menuSec != null && menuSec.isConfigurationSection("buttons.back")) {
            ConfigurationSection backSec = menuSec.getConfigurationSection("buttons.back");
            int backSlot = backSec.getInt("slot", 45);
            Material backMat = Material.matchMaterial(backSec.getString("material", "ARROW"));
            if (backMat == null) backMat = Material.ARROW;
            String backName = backSec.getString("name", "<!italic><red><b>◀ Назад</b></red>");
            List<String> backLore = backSec.getStringList("lore");

            ItemStack backItem = createSimpleItem(backMat, backName, backLore);
            if (backSlot >= 0 && backSlot < size) {
                inv.setItem(backSlot, backItem);
                holder.setHandler(backSlot, event -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    new DimensionMenu(plugin).open(player);
                });
            }
        }

        // Setup previous page button
        if (currentPage > 1 && menuSec != null && menuSec.isConfigurationSection("buttons.previous_page")) {
            ConfigurationSection prevSec = menuSec.getConfigurationSection("buttons.previous_page");
            int prevSlot = prevSec.getInt("slot", 48);
            Material prevMat = Material.matchMaterial(prevSec.getString("material", "SPECTRAL_ARROW"));
            if (prevMat == null) prevMat = Material.SPECTRAL_ARROW;
            String prevName = prevSec.getString("name", "<!italic><yellow><b>◀ Предыдущая страница</b></yellow>");
            List<String> prevLore = new ArrayList<>(prevSec.getStringList("lore"));
            prevLore.replaceAll(line -> line
                    .replace("{prev_page}", String.valueOf(currentPage - 1))
                    .replace("{total_pages}", String.valueOf(totalPages)));

            ItemStack prevItem = createSimpleItem(prevMat, prevName, prevLore);
            if (prevSlot >= 0 && prevSlot < size) {
                inv.setItem(prevSlot, prevItem);
                holder.setHandler(prevSlot, event -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    new StructureMenu(plugin, dimension, currentPage - 1).open(player);
                });
            }
        }

        // Setup next page button
        if (currentPage < totalPages && menuSec != null && menuSec.isConfigurationSection("buttons.next_page")) {
            ConfigurationSection nextSec = menuSec.getConfigurationSection("buttons.next_page");
            int nextSlot = nextSec.getInt("slot", 50);
            Material nextMat = Material.matchMaterial(nextSec.getString("material", "SPECTRAL_ARROW"));
            if (nextMat == null) nextMat = Material.SPECTRAL_ARROW;
            String nextName = nextSec.getString("name", "<!italic><yellow><b>Следующая страница ▶</b></yellow>");
            List<String> nextLore = new ArrayList<>(nextSec.getStringList("lore"));
            nextLore.replaceAll(line -> line
                    .replace("{next_page}", String.valueOf(currentPage + 1))
                    .replace("{total_pages}", String.valueOf(totalPages)));

            ItemStack nextItem = createSimpleItem(nextMat, nextName, nextLore);
            if (nextSlot >= 0 && nextSlot < size) {
                inv.setItem(nextSlot, nextItem);
                holder.setHandler(nextSlot, event -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    new StructureMenu(plugin, dimension, currentPage + 1).open(player);
                });
            }
        }

        player.openInventory(inv);
    }

    private ItemStack buildStructureItem(@Nullable ConfigurationSection menuSec, @NotNull StructureLootTable table) {
        Material mat = table.getIcon();
        if (mat == null || mat == Material.AIR || mat == Material.CAVE_AIR || mat == Material.VOID_AIR) mat = Material.CHEST;

        String name = "<!italic><gold><b>" + table.getName() + "</b></gold>";
        List<String> lore = null;

        if (menuSec != null && menuSec.isConfigurationSection("item_format")) {
            ConfigurationSection formatSec = menuSec.getConfigurationSection("item_format");
            if (formatSec.isString("name")) {
                name = formatSec.getString("name", name)
                        .replace("{name}", table.getName())
                        .replace("{id}", table.getId());
            }
            if (formatSec.isList("lore")) {
                lore = new ArrayList<>(formatSec.getStringList("lore"));
                for (int i = 0; i < lore.size(); i++) {
                    lore.set(i, lore.get(i)
                            .replace("{name}", table.getName())
                            .replace("{id}", table.getId())
                            .replace("{loot_table}", table.getLootTableKey())
                            .replace("{mode}", table.getMode().name())
                            .replace("{min_rolls}", String.valueOf(table.getMinRolls()))
                            .replace("{max_rolls}", String.valueOf(table.getMaxRolls()))
                            .replace("{items_count}", String.valueOf(table.getItems().size())));
                }
            }
        }

        return createSimpleItem(mat, name, lore);
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
