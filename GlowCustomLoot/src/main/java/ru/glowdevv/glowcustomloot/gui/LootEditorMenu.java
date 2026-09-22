package ru.glowdevv.glowcustomloot.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.glowdevv.glowcustomloot.GlowCustomLootPlugin;
import ru.glowdevv.glowcustomloot.model.DimensionType;
import ru.glowdevv.glowcustomloot.model.LootItem;
import ru.glowdevv.glowcustomloot.model.LootMode;
import ru.glowdevv.glowcustomloot.model.StructureLootTable;
import ru.glowdevv.glowcustomloot.util.TextUtil;

import java.util.ArrayList;
import java.util.List;

public class LootEditorMenu {
    private final GlowCustomLootPlugin plugin;
    private final StructureLootTable table;
    private final DimensionType dimension;
    private final int structurePage;
    private final int editorPage;

    public LootEditorMenu(@NotNull GlowCustomLootPlugin plugin, @NotNull StructureLootTable table,
                          @NotNull DimensionType dimension, int structurePage, int editorPage) {
        this.plugin = plugin;
        this.table = table;
        this.dimension = dimension;
        this.structurePage = Math.max(1, structurePage);
        this.editorPage = Math.max(1, editorPage);
    }

    public void open(@NotNull Player player) {
        ConfigurationSection menuSec = plugin.getConfigManager().getMenus().getConfigurationSection("loot_editor");
        int size = menuSec != null ? menuSec.getInt("size", 54) : 54;
        if (size != 54) size = 54;

        String rawTitle = menuSec != null ? menuSec.getString("title", "Редактор: {structure}") : "Редактор: {structure}";
        rawTitle = rawTitle.replace("{structure}", table.getName());
        Component titleComp = TextUtil.parse(rawTitle, false);

        MenuHolder holder = new MenuHolder("LOOT_EDITOR_" + table.getId());
        Inventory inv = Bukkit.createInventory(holder, size, titleComp);
        holder.setInventory(inv);

        // Fill bottom bar
        ConfigurationSection bottomSec = menuSec != null ? menuSec.getConfigurationSection("bottom_bar") : null;
        if (bottomSec != null && bottomSec.isConfigurationSection("filler")) {
            String fillerMatName = bottomSec.getString("filler.material", "BLACK_STAINED_GLASS_PANE");
            Material fillerMat = Material.matchMaterial(fillerMatName);
            if (fillerMat == null) fillerMat = Material.BLACK_STAINED_GLASS_PANE;
            String fillerName = bottomSec.getString("filler.name", " ");
            ItemStack fillerItem = createSimpleItem(fillerMat, fillerName, null);

            for (int i = 45; i < 54; i++) {
                inv.setItem(i, fillerItem);
            }
        }

        // Display loot items (slots 0-44)
        int maxLootSlots = menuSec != null ? menuSec.getInt("loot_slots_max", 45) : 45;
        List<LootItem> items = table.getItems();
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / (double) maxLootSlots));
        int currentPage = Math.clamp(this.editorPage, 1, totalPages);

        int startIndex = (currentPage - 1) * maxLootSlots;
        int endIndex = Math.min(startIndex + maxLootSlots, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            LootItem lootItem = items.get(i);
            int slot = i - startIndex;

            ItemStack displayItem = buildDisplayItem(menuSec, lootItem);
            inv.setItem(slot, displayItem);

            holder.setHandler(slot, event -> {
                ClickType click = event.getClick();
                if (click == ClickType.LEFT) {
                    lootItem.addChance(1.0);
                    playPling(player);
                    refresh(player, currentPage);
                } else if (click == ClickType.RIGHT) {
                    lootItem.addChance(10.0);
                    playPling(player);
                    refresh(player, currentPage);
                } else if (click == ClickType.SHIFT_LEFT) {
                    double newChance = lootItem.getChance() - 1.0;
                    if (newChance <= 0.05) {
                        table.removeItem(lootItem.getId());
                        playBreak(player);
                    } else {
                        lootItem.setChance(newChance);
                        playPling(player);
                    }
                    refresh(player, currentPage);
                } else if (click == ClickType.SHIFT_RIGHT) {
                    double newChance = lootItem.getChance() - 10.0;
                    if (newChance <= 0.05) {
                        table.removeItem(lootItem.getId());
                        playBreak(player);
                    } else {
                        lootItem.setChance(newChance);
                        playPling(player);
                    }
                    refresh(player, currentPage);
                } else if (click == ClickType.MIDDLE) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                    new AmountEditorMenu(plugin, table, lootItem, dimension, structurePage, currentPage).open(player);
                } else if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
                    table.removeItem(lootItem.getId());
                    playBreak(player);
                    refresh(player, currentPage);
                }
            });
        }

        // Setup bottom bar buttons
        setupBottomBar(inv, holder, player, bottomSec, currentPage, totalPages);

        // Setup quick add from player inventory
        holder.setPlayerInventoryClickHandler(event -> {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || clicked.getAmount() <= 0) return;

            String provider = plugin.getItemProviderRegistry().detectProvider(clicked);
            String newId = table.nextAvailableId();
            double defaultChance = 25.0;
            int maxAmount = Math.max(1, clicked.getAmount());

            LootItem newItem = new LootItem(newId, provider, defaultChance, 1, maxAmount, clicked);
            table.addItem(newItem);

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
            player.sendMessage(plugin.getConfigManager().getMessage("item-added",
                    "{structure}", table.getName(),
                    "{chance}", String.valueOf(defaultChance)));

            refresh(player, currentPage);
        });

        // Close handler: auto save if dirty
        holder.setCloseHandler(event -> {
            if (table.isDirty()) {
                plugin.getLootTableManager().saveTableAsync(table);
            }
        });

        player.openInventory(inv);
    }

    private void setupBottomBar(@NotNull Inventory inv, @NotNull MenuHolder holder,
                                @NotNull Player player, @Nullable ConfigurationSection bottomSec,
                                int currentPage, int totalPages) {
        if (bottomSec == null) return;

        // Back button
        if (bottomSec.isConfigurationSection("back")) {
            ConfigurationSection backSec = bottomSec.getConfigurationSection("back");
            int slot = backSec.getInt("slot", 45);
            Material mat = Material.matchMaterial(backSec.getString("material", "ARROW"));
            if (mat == null) mat = Material.ARROW;
            String name = backSec.getString("name", "<!italic><red><b>◀ Назад к списку данжей</b></red>");
            List<String> lore = backSec.getStringList("lore");

            ItemStack item = createSimpleItem(mat, name, lore);
            inv.setItem(slot, item);
            holder.setHandler(slot, event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new StructureMenu(plugin, dimension, structurePage).open(player);
            });
        }

        // Mode toggle button
        if (bottomSec.isConfigurationSection("mode_toggle")) {
            ConfigurationSection modeSec = bottomSec.getConfigurationSection("mode_toggle");
            int slot = modeSec.getInt("slot", 48);
            Material mat = Material.matchMaterial(modeSec.getString("material", "HOPPER"));
            if (mat == null) mat = Material.HOPPER;
            String name = modeSec.getString("name", "<!italic><gold><b>Режим генерации:</b> <yellow>{mode}</yellow></gold>")
                    .replace("{mode}", table.getMode().name());
            List<String> lore = new ArrayList<>(modeSec.getStringList("lore"));
            lore.replaceAll(line -> line.replace("{mode}", table.getMode().name()));

            ItemStack item = createSimpleItem(mat, name, lore);
            inv.setItem(slot, item);
            holder.setHandler(slot, event -> {
                LootMode nextMode = table.getMode() == LootMode.MERGE ? LootMode.REPLACE : LootMode.MERGE;
                table.setMode(nextMode);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                refresh(player, currentPage);
            });
        }

        // Rolls config button
        if (bottomSec.isConfigurationSection("rolls")) {
            ConfigurationSection rollsSec = bottomSec.getConfigurationSection("rolls");
            int slot = rollsSec.getInt("slot", 49);
            Material mat = Material.matchMaterial(rollsSec.getString("material", "DISPENSER"));
            if (mat == null) mat = Material.DISPENSER;
            String name = rollsSec.getString("name", "<!italic><gold><b>Количество роллов:</b> <white>{min_rolls} - {max_rolls}</white></gold>")
                    .replace("{min_rolls}", String.valueOf(table.getMinRolls()))
                    .replace("{max_rolls}", String.valueOf(table.getMaxRolls()));
            List<String> lore = rollsSec.getStringList("lore");

            ItemStack item = createSimpleItem(mat, name, lore);
            inv.setItem(slot, item);
            holder.setHandler(slot, event -> {
                ClickType click = event.getClick();
                if (click == ClickType.LEFT) {
                    table.setMinRolls(table.getMinRolls() + 1);
                } else if (click == ClickType.RIGHT) {
                    table.setMaxRolls(table.getMaxRolls() + 1);
                } else if (click == ClickType.SHIFT_LEFT) {
                    table.setMinRolls(table.getMinRolls() - 1);
                } else if (click == ClickType.SHIFT_RIGHT) {
                    table.setMaxRolls(table.getMaxRolls() - 1);
                }
                playPling(player);
                refresh(player, currentPage);
            });
        }

        // Quick add info item
        if (bottomSec.isConfigurationSection("quick_add_info")) {
            ConfigurationSection infoSec = bottomSec.getConfigurationSection("quick_add_info");
            int slot = infoSec.getInt("slot", 52);
            Material mat = Material.matchMaterial(infoSec.getString("material", "BOOK"));
            if (mat == null) mat = Material.BOOK;
            String name = infoSec.getString("name", "<!italic><green><b>Добавление предмета</b></green>");
            List<String> lore = infoSec.getStringList("lore");

            ItemStack item = createSimpleItem(mat, name, lore);
            inv.setItem(slot, item);
        }

        // Save button
        if (bottomSec.isConfigurationSection("save")) {
            ConfigurationSection saveSec = bottomSec.getConfigurationSection("save");
            int slot = saveSec.getInt("slot", 53);
            Material mat = Material.matchMaterial(saveSec.getString("material", "EMERALD"));
            if (mat == null) mat = Material.EMERALD;
            String name = saveSec.getString("name", "<!italic><green><b>Сохранить</b></green>");
            List<String> lore = saveSec.getStringList("lore");

            ItemStack item = createSimpleItem(mat, name, lore);
            inv.setItem(slot, item);
            holder.setHandler(slot, event -> {
                plugin.getLootTableManager().saveTableAsync(table);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
                player.sendMessage(plugin.getConfigManager().getMessage("saved-table",
                        "{structure}", table.getName()));
            });
        }

        // Page navigation if multi-page items
        if (totalPages > 1) {
            if (currentPage > 1) {
                ItemStack prev = createSimpleItem(Material.SPECTRAL_ARROW, "<!italic><yellow><b>◀ Пред. страница</b></yellow>", null);
                inv.setItem(46, prev);
                holder.setHandler(46, event -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    new LootEditorMenu(plugin, table, dimension, structurePage, currentPage - 1).open(player);
                });
            }
            if (currentPage < totalPages) {
                ItemStack next = createSimpleItem(Material.SPECTRAL_ARROW, "<!italic><yellow><b>След. страница ▶</b></yellow>", null);
                inv.setItem(47, next);
                holder.setHandler(47, event -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    new LootEditorMenu(plugin, table, dimension, structurePage, currentPage + 1).open(player);
                });
            }
        }
    }

    private ItemStack buildDisplayItem(@Nullable ConfigurationSection menuSec, @NotNull LootItem lootItem) {
        ItemStack item = lootItem.getTemplate().clone();
        item.editMeta(meta -> {
            List<Component> newLore = new ArrayList<>();
            if (meta.hasLore() && meta.lore() != null) {
                newLore.addAll(meta.lore());
            }

            if (menuSec != null && menuSec.isConfigurationSection("lore_format")) {
                ConfigurationSection formatSec = menuSec.getConfigurationSection("lore_format");
                List<String> header = formatSec.getStringList("header");
                for (String line : header) {
                    newLore.add(TextUtil.parse(line
                            .replace("{chance}", String.valueOf(lootItem.getChance()))
                            .replace("{min}", String.valueOf(lootItem.getMinAmount()))
                            .replace("{max}", String.valueOf(lootItem.getMaxAmount())), true));
                }

                List<String> controls = formatSec.getStringList("controls");
                for (String line : controls) {
                    newLore.add(TextUtil.parse(line, true));
                }
            } else {
                newLore.add(TextUtil.parse("<dark_gray>------------------------</dark_gray>", true));
                newLore.add(TextUtil.parse("<gray>Шанс: <yellow>" + lootItem.getChance() + "%</yellow></gray>", true));
                newLore.add(TextUtil.parse("<gray>Кол-во: <white>" + lootItem.getMinAmount() + " - " + lootItem.getMaxAmount() + " шт.</white></gray>", true));
                newLore.add(TextUtil.parse("<dark_gray>------------------------</dark_gray>", true));
            }

            meta.lore(newLore);
        });
        return item;
    }

    private void refresh(@NotNull Player player, int page) {
        new LootEditorMenu(plugin, table, dimension, structurePage, page).open(player);
    }

    private void playPling(@NotNull Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.3f);
    }

    private void playBreak(@NotNull Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 1.0f);
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
