package glowseller.menu;

import glowseller.Main;
import glowseller.configs.impl.ShopConfig;
import glowseller.models.PlayerData;
import glowseller.models.ShopItem;
import glowseller.utils.Colorizer;
import glowseller.utils.HeadUtil;
import glowseller.utils.ItemBuilder;
import glowseller.utils.ModelDataUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ShopCategoryMenu extends AbstractMenu {
    private final ShopConfig.Category category;

    public ShopCategoryMenu(Main plugin, Player player, ShopConfig.Category category) {
        super(plugin, player);
        this.category = category;
    }

    @Override
    public String getTitle() {
        return Colorizer.colorize(category.getName());
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void build() {
        // Decorate background
        Material decMat = plugin.getShopConfig().getDecorateMaterial();
        ItemStack filler = new ItemBuilder(decMat != null ? decMat : Material.GRAY_STAINED_GLASS_PANE).name("&7 ").build();
        fill(filler);

        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());

        // Balance item (slot 4)
        ItemStack balanceIcon = new ItemBuilder(Material.SUNFLOWER)
                .name("&#54A0F4&lВаши очки")
                .lore(
                        "&7Баланс: &b" + plugin.getNumberFormatManager().formatNumber(data.getPoints()) + " очков"
                ).build();
        setItem(4, balanceIcon);

        // Render category items
        for (ShopItem item : category.getItems().values()) {
            ItemStack stack;
            if (item.getBaseHeadTexture() != null && !item.getBaseHeadTexture().isEmpty()) {
                stack = HeadUtil.createHeadFromBase64(item.getBaseHeadTexture());
            } else {
                stack = new ItemStack(item.getMaterial());
            }

            if (item.getCustomModelData() != null) {
                ModelDataUtil.setCustomModelData(stack, item.getCustomModelData());
            }

            ItemBuilder builder = new ItemBuilder(stack).name(item.getName());
            List<String> lore = new ArrayList<>(item.getLore());
            lore.add("");
            lore.add("&7Цена: &b" + plugin.getNumberFormatManager().formatNumber(item.getPrice()) + " очков");

            if (item.getType() == ShopItem.Type.BOOSTER) {
                lore.add("&7Множитель монет: &ex" + String.format("%.1f", item.getMultiplier()));
                lore.add("&7Длительность: &f" + plugin.getBoosterManager().formatTime(item.getDurationSeconds()));
            } else if (item.getType() == ShopItem.Type.ITEM) {
                lore.add("&7Количество: &f" + item.getGiveAmount() + " шт.");
            }

            lore.add("");
            if (data.getPoints() >= item.getPrice()) {
                lore.add("&f▶ &eЛКМ &7— купить");
            } else {
                lore.add("&c▶ Недостаточно очков");
            }

            builder.lore(lore);

            setItem(item.getSlot(), builder.build(), e -> {
                boolean success = plugin.getShopManager().purchase(player, item.getKey());
                if (success) {
                    new ShopCategoryMenu(plugin, player, category).open();
                }
            });
        }

        // Back button (slot 22)
        ItemStack backIcon = new ItemBuilder(Material.ARROW)
                .name("&e← Назад в категории")
                .lore("&7Вернуться к списку категорий")
                .build();
        setItem(22, backIcon, e -> {
            new ShopMenu(plugin, player).open();
        });
    }
}
