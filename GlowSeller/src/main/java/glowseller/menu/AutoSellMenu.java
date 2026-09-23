package glowseller.menu;

import glowseller.Main;
import glowseller.models.PlayerData;
import glowseller.models.item.Item;
import glowseller.utils.Colorizer;
import glowseller.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AutoSellMenu extends AbstractMenu {
    private static final int[] SEPARATOR_SLOTS = {1, 10, 19, 28, 37, 38, 39, 40, 41, 42, 43, 44, 45};
    private static final int[] CONTENT_SLOTS = {
            2, 3, 4, 5, 6, 7, 8,
            11, 12, 13, 14, 15, 16, 17,
            20, 21, 22, 23, 24, 25, 26,
            29, 30, 31, 32, 33, 34, 35
    };

    private static class CategoryDef {
        final String id;
        final String name;
        final int navSlot;
        final int bonusPercent;

        CategoryDef(String id, String name, int navSlot, int bonusPercent) {
            this.id = id;
            this.name = name;
            this.navSlot = navSlot;
            this.bonusPercent = bonusPercent;
        }
    }

    private static final List<CategoryDef> CATEGORIES = Arrays.asList(
            new CategoryDef("mine", "Ресурсы с Шахты", 0, 14),
            new CategoryDef("blocks", "Блоки", 9, 50),
            new CategoryDef("plants", "Растительность", 18, 25),
            new CategoryDef("mobs", "Лут с Мобов", 27, 25),
            new CategoryDef("misc", "Разное", 36, 50)
    );

    // Dynamic thematic materials for slots [0, 9, 18, 27, 36] per active category index
    // Matching DeluxeMenus extract files:
    // Menus 0 (Mine) & 1 (Blocks): COAL, OAK_LOG, NETHER_WART, SUGAR, POTION
    // Menus 2 (Plants), 3 (Mobs), 4 (Misc): QUARTZ, END_STONE, CACTUS, COOKED_PORKCHOP, PUFFERFISH
    private static final Material[][] NAV_ICONS = {
            // Category 0: Mine
            {Material.COAL, Material.OAK_LOG, Material.NETHER_WART, Material.SUGAR, Material.POTION},
            // Category 1: Blocks
            {Material.COAL, Material.OAK_LOG, Material.NETHER_WART, Material.SUGAR, Material.POTION},
            // Category 2: Plants
            {Material.QUARTZ, Material.END_STONE, Material.CACTUS, Material.COOKED_PORKCHOP, Material.PUFFERFISH},
            // Category 3: Mobs
            {Material.QUARTZ, Material.END_STONE, Material.CACTUS, Material.COOKED_PORKCHOP, Material.PUFFERFISH},
            // Category 4: Misc
            {Material.QUARTZ, Material.END_STONE, Material.CACTUS, Material.COOKED_PORKCHOP, Material.PUFFERFISH}
    };

    private int activeCategoryIndex = 0;

    public AutoSellMenu(Main plugin, Player player) {
        this(plugin, player, 0);
    }

    public AutoSellMenu(Main plugin, Player player, int categoryIndex) {
        super(plugin, player);
        this.activeCategoryIndex = Math.max(0, Math.min(categoryIndex, CATEGORIES.size() - 1));
    }

    @Override
    public String getTitle() {
        return Colorizer.colorize("Фильтр Авто-продаж");
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    public void build() {
        // 1. Separators
        ItemStack separator = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int slot : SEPARATOR_SLOTS) {
            setItem(slot, separator);
        }

        // 2. Navigation Categories (Left Column: 0, 9, 18, 27, 36)
        renderCategorySelectors();

        // 3. Middle Content Grid
        renderCategoryItems();

        // 4. Bottom Controls: 46 (Enable All), 47 (Disable All), 49 (Comparator Mode), 51 (Bonus), 53 (Back)
        renderBottomControls();
    }

    private void renderCategorySelectors() {
        for (int i = 0; i < CATEGORIES.size(); i++) {
            CategoryDef cat = CATEGORIES.get(i);
            final int index = i;
            boolean isActive = (i == activeCategoryIndex);

            Material iconMat = NAV_ICONS[activeCategoryIndex][i];

            ItemBuilder builder = new ItemBuilder(iconMat)
                    .name(" &#00D8FF❏ &#FFFFFFКатегория: &#00D8FF" + cat.name + "&#00D8FF ❏")
                    .lore(
                            "",
                            " &#00D8FF● &#FFFFFFНакоплено очков: &#05FB000",
                            "",
                            " &#00D8FF&l&n▍&#FFFFFF Бонус &#FFC900+" + cat.bonusPercent + "% монет ¤&#FFFFFF, если",
                            " &#00D8FF&l▍&#FFFFFF эта категория станет ТОП #1",
                            "",
                            isActive ? " &#05FB00● &#FFFFFFТекущая категория" : " &#00D8FF▶ &#FFFFFFНажмите, чтобы открыть категорию"
                    );

            if (isActive) {
                builder.glow();
            }

            setItem(cat.navSlot, builder.build(), e -> {
                if (activeCategoryIndex != index) {
                    activeCategoryIndex = index;
                    renderCategorySelectors();
                    renderCategoryItems();
                    renderBottomControls();
                }
            });
        }
    }

    private void renderCategoryItems() {
        CategoryDef cat = CATEGORIES.get(activeCategoryIndex);
        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());

        // Clear content slots first
        for (int slot : CONTENT_SLOTS) {
            setItem(slot, null, null);
        }

        List<Item> categoryItems = new ArrayList<>();
        for (Item item : plugin.getItemManager().getItems().values()) {
            if (cat.id.equalsIgnoreCase(item.getCategory())) {
                categoryItems.add(item);
            }
        }

        int slotIdx = 0;
        for (Item item : categoryItems) {
            if (slotIdx >= CONTENT_SLOTS.length) break;
            int slot = CONTENT_SLOTS[slotIdx++];

            boolean enabled = data.isItemAutoSellEnabled(item.getId());

            ItemBuilder builder = new ItemBuilder(item.getMaterial())
                    .name(item.getName())
                    .lore(
                            "",
                            " &#FF7000&l&n▍&#FFFFFF Стандартная цена: &#FFC900" + plugin.getNumberFormatManager().formatNumber(item.getPrice()) + "&#FFC900 монет ¤",
                            " &#FF7000&l▍&#FFFFFF Очки за сдачу: &#05FB00+" + plugin.getNumberFormatManager().formatNumber(item.getPoints()),
                            "",
                            enabled ? " &#05FB00✔ &#FFFFFFАвтосдача: &#05FB00ВКЛЮЧЕНА" : " &#FF2222✘ &#FFFFFFАвтосдача: &#FF2222ВЫКЛЮЧЕНА",
                            enabled ? " &#00D8FF▶ &#FFFFFFНажмите, чтобы &#FF2222отключить" : " &#00D8FF▶ &#FFFFFFНажмите, чтобы &#05FB00включить"
                    );

            if (enabled) {
                builder.glow();
            }

            setItem(slot, builder.build(), e -> {
                data.toggleItemAutoSell(item.getId());
                try {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                } catch (Throwable ignored) {}
                renderCategoryItems();
            });
        }
    }

    private void renderBottomControls() {
        CategoryDef cat = CATEGORIES.get(activeCategoryIndex);
        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());

        // Slot 46: Lime Dye (Enable All in Category)
        ItemStack enableAll = new ItemBuilder(Material.LIME_DYE)
                .name(" &#05FB00✔ Включить все товары")
                .lore(
                        "",
                        " &#00D8FF&l&n▍&#FFFFFF Включение авто-продажи для",
                        " &#00D8FF&l▍&#FFFFFF всех товаров из категории &#FFC900" + cat.name,
                        "",
                        " &#00D8FF▶ &#FFFFFFНажмите, чтобы включить"
                ).build();
        setItem(46, enableAll, e -> {
            for (Item item : plugin.getItemManager().getItems().values()) {
                if (cat.id.equalsIgnoreCase(item.getCategory())) {
                    data.setItemAutoSell(item.getId(), true);
                }
            }
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
            } catch (Throwable ignored) {}
            renderCategoryItems();
        });

        // Slot 47: Red Dye (Disable All in Category)
        ItemStack disableAll = new ItemBuilder(Material.RED_DYE)
                .name(" &#FF2222✘ Отключить все товары")
                .lore(
                        "",
                        " &#00D8FF&l&n▍&#FFFFFF Отключение авто-продажи для",
                        " &#00D8FF&l▍&#FFFFFF всех товаров из категории &#FFC900" + cat.name,
                        "",
                        " &#00D8FF▶ &#FFFFFFНажмите, чтобы отключить"
                ).build();
        setItem(47, disableAll, e -> {
            for (Item item : plugin.getItemManager().getItems().values()) {
                if (cat.id.equalsIgnoreCase(item.getCategory())) {
                    data.setItemAutoSell(item.getId(), false);
                }
            }
            try {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.8f);
            } catch (Throwable ignored) {}
            renderCategoryItems();
        });

        // Slot 49: Comparator (Toggle Mode: OFF -> ENABLED -> ENABLED_SESSION -> OFF)
        PlayerData.AutoSellMode mode = data.getAutoSellMode();
        List<String> modeLore = new ArrayList<>();
        modeLore.add(mode == PlayerData.AutoSellMode.ENABLED ? " &#9CF9FF● &#FFFFFFВключено" : " &8● Включено");
        modeLore.add(mode == PlayerData.AutoSellMode.ENABLED_SESSION ? " &#9CF9FF● &#FFFFFFВключено (до выхода)" : " &8● Включено (до выхода)");
        modeLore.add(mode == PlayerData.AutoSellMode.OFF ? " &#FF7000✔ &#FFFFFFВыключено" : " &8● Выключено");
        modeLore.add("");
        modeLore.add(" &#00D8FF▶ &#FFFFFFНажмите, чтобы переключить режим");

        ItemStack comparator = new ItemBuilder(Material.COMPARATOR)
                .name(" &#00D8FFАвто-продажа")
                .lore(modeLore)
                .build();
        setItem(49, comparator, e -> {
            PlayerData.AutoSellMode nextMode = switch (data.getAutoSellMode()) {
                case OFF -> PlayerData.AutoSellMode.ENABLED;
                case ENABLED -> PlayerData.AutoSellMode.ENABLED_SESSION;
                case ENABLED_SESSION -> PlayerData.AutoSellMode.OFF;
            };
            data.setAutoSellMode(nextMode);
            try {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
            } catch (Throwable ignored) {}
            renderBottomControls();
        });

        // Slot 51: Prismarine Crystals (Info)
        ItemStack bonus = new ItemBuilder(Material.PRISMARINE_CRYSTALS)
                .name(" &#FF7000Повысить локальный бонус")
                .lore(
                        "",
                        " &#FF7000&l&n▍&#FFFFFF Вы получите дополнительно &#FFC900+20.0%&#FFFFFF к",
                        " &#FF7000&l&n▍&#FFFFFF локальному бонусу случайного товара",
                        " &#FF7000&l▍&#FFFFFF из категории &#FF7000" + cat.name,
                        "",
                        " &#FF7000● &#FFFFFFТекущий бонус: &#05FB00+0.0%",
                        "",
                        " &#FF7000▶ &#FFFFFFНажмите, чтобы купить повышение",
                        "    &#FFFFFFза x&#FFFFFF12&#FFFFFF &#992900Б&#A82D00о&#B83100е&#C83500в&#D73A00о&#E73E00й &#F74200ф&#F74200р&#E73E00а&#D73A00г&#C83500м&#B83100е&#A82D00н&#992900т"
                ).build();
        setItem(51, bonus);

        // Slot 53: Arrow Back to Seller
        ItemStack back = new ItemBuilder(Material.ARROW)
                .name("&#FF2222◀ &#FFFFFFНазад")
                .build();
        setItem(53, back, e -> new SellMenu(plugin, player).open());
    }
}
