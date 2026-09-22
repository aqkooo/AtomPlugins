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
import ru.glowdevv.glowcustomloot.model.LootItem;
import ru.glowdevv.glowcustomloot.model.StructureLootTable;
import ru.glowdevv.glowcustomloot.util.TextUtil;

import java.util.ArrayList;
import java.util.List;

public class AmountEditorMenu {
    private final GlowCustomLootPlugin plugin;
    private final StructureLootTable table;
    private final LootItem lootItem;
    private final DimensionType dimension;
    private final int structurePage;
    private final int editorPage;

    public AmountEditorMenu(@NotNull GlowCustomLootPlugin plugin, @NotNull StructureLootTable table,
                            @NotNull LootItem lootItem, @NotNull DimensionType dimension,
                            int structurePage, int editorPage) {
        this.plugin = plugin;
        this.table = table;
        this.lootItem = lootItem;
        this.dimension = dimension;
        this.structurePage = structurePage;
        this.editorPage = editorPage;
    }

    public void open(@NotNull Player player) {
        ConfigurationSection menuSec = plugin.getConfigManager().getMenus().getConfigurationSection("amount_editor");
        int size = menuSec != null ? menuSec.getInt("size", 27) : 27;
        if (size != 27) size = 27;

        String itemName = lootItem.getTemplate().getType().name();
        if (lootItem.getTemplate().hasItemMeta() && lootItem.getTemplate().getItemMeta().hasDisplayName()) {
            itemName = TextUtil.toPlain(lootItem.getTemplate().getItemMeta().displayName());
        }

        String rawTitle = menuSec != null ? menuSec.getString("title", "Количество: {item}") : "Количество: {item}";
        rawTitle = rawTitle.replace("{item}", itemName);
        Component titleComp = TextUtil.parse(rawTitle, false);

        MenuHolder holder = new MenuHolder("AMOUNT_EDITOR_" + lootItem.getId());
        Inventory inv = Bukkit.createInventory(holder, size, titleComp);
        holder.setInventory(inv);

        // Background filler
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

        // Slot 4: Item display
        int displaySlot = menuSec != null ? menuSec.getInt("item_display_slot", 4) : 4;
        ItemStack preview = lootItem.getTemplate().clone();
        preview.editMeta(meta -> {
            List<Component> lore = new ArrayList<>();
            if (meta.hasLore() && meta.lore() != null) {
                lore.addAll(meta.lore());
            }
            lore.add(TextUtil.parse("<dark_gray>------------------------</dark_gray>", true));
            lore.add(TextUtil.parse("<gray>Минимум: <yellow>" + lootItem.getMinAmount() + " шт.</yellow></gray>", true));
            lore.add(TextUtil.parse("<gray>Максимум: <yellow>" + lootItem.getMaxAmount() + " шт.</yellow></gray>", true));
            lore.add(TextUtil.parse("<dark_gray>------------------------</dark_gray>", true));
            meta.lore(lore);
        });
        inv.setItem(displaySlot, preview);

        // Buttons
        ConfigurationSection btnSec = menuSec != null ? menuSec.getConfigurationSection("buttons") : null;

        // Min buttons
        setupActionButton(inv, holder, player, btnSec, "min_minus_5", 10, Material.RED_STAINED_GLASS_PANE,
                "<!italic><red><b>-5 к Мин</b></red>", () -> {
                    lootItem.setMinAmount(Math.max(1, lootItem.getMinAmount() - 5));
                    table.setDirty(true);
                    refresh(player);
                });

        setupActionButton(inv, holder, player, btnSec, "min_minus_1", 11, Material.RED_CONCRETE,
                "<!italic><red><b>-1 к Мин</b></red>", () -> {
                    lootItem.setMinAmount(Math.max(1, lootItem.getMinAmount() - 1));
                    table.setDirty(true);
                    refresh(player);
                });

        // Current min display
        setupDisplayInfo(inv, btnSec, "min_current", 12, Material.PAPER,
                "<!italic><gray>Минимум: <yellow><b>" + lootItem.getMinAmount() + "</b></yellow></gray>");

        setupActionButton(inv, holder, player, btnSec, "min_plus_1", 13, Material.LIME_CONCRETE,
                "<!italic><green><b>+1 к Мин</b></green>", () -> {
                    lootItem.setMinAmount(lootItem.getMinAmount() + 1);
                    table.setDirty(true);
                    refresh(player);
                });

        setupActionButton(inv, holder, player, btnSec, "min_plus_5", 14, Material.LIME_STAINED_GLASS_PANE,
                "<!italic><green><b>+5 к Мин</b></green>", () -> {
                    lootItem.setMinAmount(lootItem.getMinAmount() + 5);
                    table.setDirty(true);
                    refresh(player);
                });

        // Max buttons
        setupActionButton(inv, holder, player, btnSec, "max_minus_5", 19, Material.RED_STAINED_GLASS_PANE,
                "<!italic><red><b>-5 к Макс</b></red>", () -> {
                    lootItem.setMaxAmount(Math.max(1, lootItem.getMaxAmount() - 5));
                    table.setDirty(true);
                    refresh(player);
                });

        setupActionButton(inv, holder, player, btnSec, "max_minus_1", 20, Material.RED_CONCRETE,
                "<!italic><red><b>-1 к Макс</b></red>", () -> {
                    lootItem.setMaxAmount(Math.max(1, lootItem.getMaxAmount() - 1));
                    table.setDirty(true);
                    refresh(player);
                });

        // Current max display
        setupDisplayInfo(inv, btnSec, "max_current", 21, Material.PAPER,
                "<!italic><gray>Максимум: <yellow><b>" + lootItem.getMaxAmount() + "</b></yellow></gray>");

        setupActionButton(inv, holder, player, btnSec, "max_plus_1", 22, Material.LIME_CONCRETE,
                "<!italic><green><b>+1 к Макс</b></green>", () -> {
                    lootItem.setMaxAmount(Math.min(64, lootItem.getMaxAmount() + 1));
                    table.setDirty(true);
                    refresh(player);
                });

        setupActionButton(inv, holder, player, btnSec, "max_plus_5", 23, Material.LIME_STAINED_GLASS_PANE,
                "<!italic><green><b>+5 к Макс</b></green>", () -> {
                    lootItem.setMaxAmount(Math.min(64, lootItem.getMaxAmount() + 5));
                    table.setDirty(true);
                    refresh(player);
                });

        // Back button
        setupActionButton(inv, holder, player, btnSec, "back", 18, Material.ARROW,
                "<!italic><red><b>Готово / Назад</b></red>", () -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    new LootEditorMenu(plugin, table, dimension, structurePage, editorPage).open(player);
                });

        holder.setCloseHandler(event -> {
            if (table.isDirty()) {
                plugin.getLootTableManager().saveTableAsync(table);
            }
        });

        player.openInventory(inv);
    }

    private void setupActionButton(@NotNull Inventory inv, @NotNull MenuHolder holder,
                                   @NotNull Player player, @Nullable ConfigurationSection btnSec,
                                   @NotNull String key, int defSlot, @NotNull Material defMat,
                                   @NotNull String defName, @NotNull Runnable action) {
        int slot = defSlot;
        Material mat = defMat;
        String name = defName;
        List<String> lore = null;

        if (btnSec != null && btnSec.isConfigurationSection(key)) {
            ConfigurationSection sec = btnSec.getConfigurationSection(key);
            slot = sec.getInt("slot", slot);
            String m = sec.getString("material");
            if (m != null) {
                Material parsed = Material.matchMaterial(m);
                if (parsed != null) mat = parsed;
            }
            if (sec.isString("name")) name = sec.getString("name");
            if (sec.isList("lore")) lore = sec.getStringList("lore");
        }

        ItemStack item = createSimpleItem(mat, name, lore);
        if (slot >= 0 && slot < inv.getSize()) {
            inv.setItem(slot, item);
            holder.setHandler(slot, event -> {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.4f);
                action.run();
            });
        }
    }

    private void setupDisplayInfo(@NotNull Inventory inv, @Nullable ConfigurationSection btnSec,
                                  @NotNull String key, int defSlot, @NotNull Material defMat,
                                  @NotNull String defName) {
        int slot = defSlot;
        Material mat = defMat;
        String name = defName;

        if (btnSec != null && btnSec.isConfigurationSection(key)) {
            ConfigurationSection sec = btnSec.getConfigurationSection(key);
            slot = sec.getInt("slot", slot);
            String m = sec.getString("material");
            if (m != null) {
                Material parsed = Material.matchMaterial(m);
                if (parsed != null) mat = parsed;
            }
            if (sec.isString("name")) {
                name = sec.getString("name")
                        .replace("{min}", String.valueOf(lootItem.getMinAmount()))
                        .replace("{max}", String.valueOf(lootItem.getMaxAmount()));
            }
        }

        ItemStack item = createSimpleItem(mat, name, null);
        if (slot >= 0 && slot < inv.getSize()) {
            inv.setItem(slot, item);
        }
    }

    private void refresh(@NotNull Player player) {
        open(player);
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
