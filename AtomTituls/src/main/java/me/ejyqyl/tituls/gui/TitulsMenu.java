package me.ejyqyl.tituls.gui;

import me.ejyqyl.tituls.AtomTitulsPlugin;
import me.ejyqyl.tituls.manager.ConfigManager;
import me.ejyqyl.tituls.manager.PlayerCache;
import me.ejyqyl.tituls.model.PlayerData;
import me.ejyqyl.tituls.model.Titul;
import me.ejyqyl.tituls.model.TitulType;
import me.ejyqyl.tituls.sorting.SortDirection;
import me.ejyqyl.tituls.sorting.SortMode;
import me.ejyqyl.tituls.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class TitulsMenu {
    private final AtomTitulsPlugin plugin;
    private final int page;
    private final UUID targetUuid;
    private final String targetName;
    private final boolean readOnly;
    private final PlayerData viewData;

    private final Map<Integer, Titul> titleSlots = new HashMap<>();
    private int previousPageSlot = -1;
    private int nextPageSlot = -1;
    private int sortingSlot = -1;
    private int clearSlot = -1;

    public TitulsMenu(@NotNull AtomTitulsPlugin plugin, int page, @Nullable UUID targetUuid, @Nullable String targetName, boolean readOnly, @Nullable PlayerData viewData) {
        this.plugin = plugin;
        this.page = Math.max(1, page);
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.readOnly = readOnly;
        this.viewData = viewData;
    }

    public TitulsMenu(@NotNull AtomTitulsPlugin plugin, int page) {
        this(plugin, page, null, null, false, null);
    }

    public TitulsMenu(@NotNull AtomTitulsPlugin plugin) {
        this(plugin, 1);
    }

    public void open(@NotNull Player viewer) {
        ConfigManager cfg = plugin.getConfigManager();
        FileConfiguration menuCfg = cfg.getMenuConfig();

        int size = menuCfg.getInt("menu.settings.size", 54);
        if (size < 9 || size > 54 || size % 9 != 0) {
            size = 54;
        }

        String displayName = targetName != null ? targetName : viewer.getName();
        String rawTitle = menuCfg.getString("menu.settings.displayName", "&0Титулы {player}");
        String menuTitle = rawTitle.replace("{player}", displayName);

        TitulsMenuHolder holder = new TitulsMenuHolder(this);
        Inventory inv = Bukkit.createInventory(holder, size, TextUtil.toComponent(menuTitle));
        holder.setInventory(inv);

        UUID viewedUuid = targetUuid != null ? targetUuid : viewer.getUniqueId();
        List<Titul> unlocked = viewData != null ? viewData.getUnlockedTituls() : plugin.getPlayerCache().getUnlockedTituls(viewedUuid);

        SortMode currentSort = resolveSortMode(viewer.getUniqueId());
        List<Titul> sorted = sortTitles(unlocked, currentSort);

        // Fill decorative panes
        loadPanes(inv, menuCfg);

        // Setup title items and pagination
        List<Integer> configuredSlots = menuCfg.getIntegerList("menu.items.titul.slots");
        if (configuredSlots.isEmpty()) {
            for (int i = 10; i < 44; i++) {
                if (i % 9 != 0 && i % 9 != 8) configuredSlots.add(i);
            }
        }

        int perPage = configuredSlots.size();
        int totalItems = sorted.size();
        int startIndex = (page - 1) * perPage;

        titleSlots.clear();
        for (int i = 0; i < perPage; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= totalItems) break;

            Titul titul = sorted.get(itemIndex);
            int slot = configuredSlots.get(i);
            if (slot >= 0 && slot < size) {
                inv.setItem(slot, buildTitulItem(titul, menuCfg));
                titleSlots.put(slot, titul);
            }
        }

        // Pagination buttons
        previousPageSlot = menuCfg.getInt("menu.items.previous_page.slot", 48);
        if (page > 1 && previousPageSlot >= 0 && previousPageSlot < size) {
            inv.setItem(previousPageSlot, buildButton(menuCfg, "menu.items.previous_page", Material.ARROW));
        }

        nextPageSlot = menuCfg.getInt("menu.items.next_page.slot", 50);
        if (startIndex + perPage < totalItems && nextPageSlot >= 0 && nextPageSlot < size) {
            inv.setItem(nextPageSlot, buildButton(menuCfg, "menu.items.next_page", Material.ARROW));
        }

        // Sorting button
        sortingSlot = menuCfg.getInt("menu.items.sorting.slot", 49);
        if (sortingSlot >= 0 && sortingSlot < size) {
            inv.setItem(sortingSlot, buildSortingItem(currentSort, totalItems, menuCfg));
        }

        // Clear active title button
        if (!readOnly) {
            clearSlot = menuCfg.getInt("menu.items.clear.slot", 46);
            if (clearSlot >= 0 && clearSlot < size) {
                inv.setItem(clearSlot, buildButton(menuCfg, "menu.items.clear", Material.RED_DYE));
            }
        }

        viewer.openInventory(inv);
    }

    public void handleClick(@NotNull InventoryClickEvent event, @NotNull Player player) {
        int rawSlot = event.getRawSlot();
        if (rawSlot != event.getSlot() || rawSlot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        PlayerCache cache = plugin.getPlayerCache();
        UUID uuid = player.getUniqueId();

        if (titleSlots.containsKey(rawSlot)) {
            Titul clickedTitul = titleSlots.get(rawSlot);
            if (readOnly) {
                return;
            }

            if (event.getClick() == ClickType.LEFT) {
                if (!cache.tryAcquireAction(uuid)) return;

                Titul active = cache.getActiveTitul(uuid);
                boolean isCurrent = active != null && active.getId().equalsIgnoreCase(clickedTitul.getId()) && active.getType() == clickedTitul.getType();

                if (isCurrent) {
                    cache.clearActiveTitul(uuid).whenComplete((v, ex) -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            cache.releaseAction(uuid);
                            if (player.isOnline()) {
                                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
                                open(player);
                            }
                        });
                    });
                } else {
                    cache.activateTitul(uuid, clickedTitul).whenComplete((v, ex) -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            cache.releaseAction(uuid);
                            if (player.isOnline()) {
                                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
                                open(player);
                            }
                        });
                    });
                }
            } else if (event.getClick() == ClickType.RIGHT) {
                if (!cache.tryAcquireAction(uuid)) return;

                cache.revokeTitul(uuid, clickedTitul.getId()).thenRun(() -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        cache.releaseAction(uuid);
                        if (!player.isOnline()) return;

                        ItemStack tagItem = plugin.getTitulTagManager().createTagItem(clickedTitul);
                        Map<Integer, ItemStack> overflow = player.getInventory().addItem(tagItem);
                        overflow.values().forEach(is -> player.getWorld().dropItemNaturally(player.getLocation(), is));

                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.0f);
                        open(player);
                    });
                }).exceptionally(ex -> {
                    plugin.getLogger().log(Level.WARNING, "Failed to revoke title for tag conversion " + clickedTitul.getId(), ex);
                    cache.releaseAction(uuid);
                    return null;
                });
            }
            return;
        }

        if (rawSlot == previousPageSlot && page > 1) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new TitulsMenu(plugin, page - 1, targetUuid, targetName, readOnly, viewData).open(player);
            return;
        }

        if (rawSlot == nextPageSlot) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new TitulsMenu(plugin, page + 1, targetUuid, targetName, readOnly, viewData).open(player);
            return;
        }

        if (rawSlot == sortingSlot) {
            SortMode current = resolveSortMode(uuid);
            SortMode next = plugin.getConfigManager().getNextSortMode(current != null ? current.getName() : null);
            if (next != null) {
                cache.setSortMode(uuid, next.getName()).thenRun(() -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.1f);
                            new TitulsMenu(plugin, 1, targetUuid, targetName, readOnly, viewData).open(player);
                        }
                    });
                });
            }
            return;
        }

        if (!readOnly && rawSlot == clearSlot) {
            cache.clearActiveTitul(uuid).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
                        open(player);
                    }
                });
            });
        }
    }

    private void loadPanes(Inventory inv, FileConfiguration menuCfg) {
        ItemStack orange = buildPane(menuCfg, "menu.items.orange_pane", Material.ORANGE_STAINED_GLASS_PANE);
        for (int slot : menuCfg.getIntegerList("menu.items.orange_pane.slots")) {
            if (slot >= 0 && slot < inv.getSize()) inv.setItem(slot, orange);
        }

        ItemStack black = buildPane(menuCfg, "menu.items.black_pane", Material.GRAY_STAINED_GLASS_PANE);
        for (int slot : menuCfg.getIntegerList("menu.items.black_pane.slots")) {
            if (slot >= 0 && slot < inv.getSize()) inv.setItem(slot, black);
        }
    }

    private ItemStack buildPane(FileConfiguration cfg, String path, Material fallback) {
        Material mat = getMaterial(cfg.getString(path + ".material"), fallback);
        String name = cfg.getString(path + ".name", "&f");
        List<String> lore = cfg.getStringList(path + ".lore");
        return TextUtil.createItem(mat, name, lore);
    }

    private ItemStack buildButton(FileConfiguration cfg, String path, Material fallback) {
        Material mat = getMaterial(cfg.getString(path + ".material"), fallback);
        String name = cfg.getString(path + ".name");
        List<String> lore = cfg.getStringList(path + ".lore");
        return TextUtil.createItem(mat, name, lore);
    }

    private ItemStack buildTitulItem(Titul titul, FileConfiguration menuCfg) {
        String basePath = readOnly ? "menu.items.view_titul" : "menu.items.titul";
        Material mat = getMaterial(menuCfg.getString(basePath + ".material"), Material.NAME_TAG);
        String nameFormat = menuCfg.getString(basePath + ".name", " {titul}");
        List<String> rawLore = menuCfg.getStringList(basePath + ".lore");

        String formattedName = nameFormat.replace("{titul}", titul.getName());

        String typeDisplay = titul.getType() == TitulType.CASE
                ? menuCfg.getString("menu.settings.type.case", "Кейсовый")
                : menuCfg.getString("menu.settings.type.custom", "Уникальный");

        List<String> formattedLore = new ArrayList<>();
        for (String line : rawLore) {
            String replaced = line
                    .replace("{type}", typeDisplay)
                    .replace("{time}", titul.getFormatted());
            formattedLore.add(replaced);
        }

        return TextUtil.createItem(mat, formattedName, formattedLore);
    }

    private ItemStack buildSortingItem(SortMode currentSort, int count, FileConfiguration menuCfg) {
        Material mat = getMaterial(menuCfg.getString("menu.items.sorting.material"), Material.HOPPER);
        String name = menuCfg.getString("menu.items.sorting.name", " &#00D8FFСортировка");
        List<String> lore = new ArrayList<>(menuCfg.getStringList("menu.items.sorting.lore"));

        for (SortMode mode : plugin.getConfigManager().getSortModes()) {
            boolean isSelected = currentSort != null && mode.getName().equalsIgnoreCase(currentSort.getName());
            lore.add(isSelected ? mode.getSelectedName() : mode.getUnselectedName());
        }

        for (String countLine : menuCfg.getStringList("menu.items.sorting.count-line")) {
            lore.add(countLine.replace("{count}", String.valueOf(count)));
        }

        return TextUtil.createItem(mat, name, lore);
    }

    private SortMode resolveSortMode(UUID uuid) {
        String modeName = plugin.getPlayerCache().getSortMode(uuid);
        return plugin.getConfigManager().getSortModeByName(modeName);
    }

    private List<Titul> sortTitles(List<Titul> list, SortMode mode) {
        if (mode == null) return list;

        Comparator<Titul> comparator;
        if ("{custom}".equalsIgnoreCase(mode.getValue())) {
            comparator = Comparator.comparingInt((Titul t) -> t.getType() == TitulType.CUSTOM ? 1 : 0)
                    .thenComparingLong(t -> t.getObtainedAt() != null ? t.getObtainedAt() : 0L);
        } else if ("{rarity}".equalsIgnoreCase(mode.getValue())) {
            comparator = Comparator.comparingInt(t -> t.getRarity().ordinal());
        } else {
            comparator = Comparator.comparingLong(t -> t.getObtainedAt() != null ? t.getObtainedAt() : 0L);
        }

        if (mode.getDirection() == SortDirection.MAX) {
            comparator = comparator.reversed();
        }

        List<Titul> sorted = new ArrayList<>(list);
        sorted.sort(comparator);
        return sorted;
    }

    private Material getMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
