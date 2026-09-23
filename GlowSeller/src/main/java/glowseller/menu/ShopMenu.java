package glowseller.menu;

import glowseller.Main;
import glowseller.models.PlayerData;
import glowseller.models.ShopItem;
import glowseller.utils.Colorizer;
import glowseller.utils.HeadUtil;
import glowseller.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class ShopMenu extends AbstractMenu {
    public ShopMenu(Main plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public String getTitle() {
        String customTitle = plugin.getMessageConfig().getRaw("gui.shop_title");
        if (customTitle != null && !customTitle.isEmpty()) {
            return Colorizer.colorize(customTitle);
        }
        return Colorizer.colorize("Магазин");
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
        // 1. Borders and decorations matching DeluxeMenus dump
        // Gray stained glass panes: slots 0, 8, 45, 53
        ItemStack grayPane = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int slot : Arrays.asList(0, 8, 45, 53)) {
            setItem(slot, grayPane);
        }

        // Orange stained glass panes: slots 9, 17, 18, 26, 27, 35, 36, 44
        ItemStack orangePane = new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).name(" ").build();
        for (int slot : Arrays.asList(9, 17, 18, 26, 27, 35, 36, 44)) {
            setItem(slot, orangePane);
        }

        // Light gray stained glass panes: slots 48, 50
        ItemStack lightGrayPane = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name(" ").build();
        setItem(48, lightGrayPane);
        setItem(50, lightGrayPane);

        // 2. Emerald showing live points balance (slot 49)
        updateBalanceEmerald();

        // 3. Arrow back to Seller Menu (slot 52)
        ItemStack backArrow = new ItemBuilder(Material.ARROW)
                .name("&#FF2222◀ &#FFFFFFНазад")
                .build();
        setItem(52, backArrow, e -> new SellMenu(plugin, player).open());

        // 4. Shop merchandise items
        for (ShopItem item : plugin.getShopConfig().getAllItems()) {
            renderShopItem(item);
        }
    }

    private void updateBalanceEmerald() {
        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());
        String formattedPoints = plugin.getNumberFormatManager().formatNumber(data.getPoints());

        ItemStack emerald = new ItemBuilder(Material.EMERALD)
                .name(" ")
                .lore(
                        " &#05FB00&l&n▍",
                        " &#05FB00&l&n▍&#FFFFFF Ваш баланс: &#05FB00" + formattedPoints + "&#05FB00 очков",
                        " &#05FB00&l&n▍",
                        " &#05FB00&l&n▍&#FFFFFF Сдавайте предметы, чтобы",
                        " &#05FB00&l&n▍&#FFFFFF заработать очки Скупщика",
                        " &#05FB00&l▍",
                        ""
                ).build();

        setItem(49, emerald);
    }

    private void renderShopItem(ShopItem item) {
        if (item.getSlot() < 0 || item.getSlot() >= getSize()) return;

        ItemStack stack;
        if (item.getBaseHeadTexture() != null && !item.getBaseHeadTexture().isEmpty()) {
            stack = HeadUtil.createHeadFromBase64(item.getBaseHeadTexture());
        } else {
            stack = new ItemStack(item.getMaterial());
        }

        ItemBuilder builder = new ItemBuilder(stack)
                .name(item.getName())
                .lore(item.getLore())
                .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

        if (item.getCustomModelData() != null) {
            builder.customModelData(item.getCustomModelData());
        }

        setItem(item.getSlot(), builder.build(), e -> {
            boolean success = plugin.getShopManager().purchase(player, item.getKey());
            if (success) {
                // Update emerald balance
                updateBalanceEmerald();
            }
        });
    }
}
