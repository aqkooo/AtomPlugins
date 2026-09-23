package glowseller.menu;

import glowseller.Main;
import glowseller.models.PlayerData;
import glowseller.utils.Colorizer;
import glowseller.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MainMenu extends AbstractMenu {
    public MainMenu(Main plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public String getTitle() {
        return Colorizer.colorize(plugin.getMessageConfig().getRaw("gui.main_title"));
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void build() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("&7 ").build();
        fill(filler);

        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());
        double totalMultiplier = plugin.getBoosterManager().getTotalMultiplier(player);

        // 1. Seller button
        ItemStack sellIcon = new ItemBuilder(Material.GOLD_INGOT)
                .name("&#54A0F4&lСкупщик предметов")
                .lore(
                        "&7Нажмите, чтобы открыть меню скупщика",
                        "&7и продать добытые ресурсы.",
                        "",
                        "&f▶ &eЛКМ &7— перейти к скупке"
                ).build();
        setItem(11, sellIcon, e -> {
            new SellMenu(plugin, player).open();
        });

        // 2. Profile & stats button
        String boosterStatus = data.hasActiveBooster()
                ? "&aАктивен &7(x" + String.format("%.1f", plugin.getBoosterManager().getActiveBoosterMultiplier(player)) + " - " + plugin.getBoosterManager().formatTime(data.getBoosterTimeLeftSeconds()) + ")"
                : "&cНе активен";

        ItemStack infoIcon = new ItemBuilder(Material.PLAYER_HEAD)
                .name("&#54A0F4&lВаш профиль")
                .lore(
                        "&7Очки магазина: &b" + plugin.getNumberFormatManager().formatNumber(data.getPoints()),
                        "&7Множитель скупщика: &e" + String.format("%.2f", totalMultiplier) + "x",
                        "&7Временный бустер: " + boosterStatus
                ).build();
        setItem(13, infoIcon);

        // 3. Shop button
        ItemStack shopIcon = new ItemBuilder(Material.EMERALD)
                .name("&#54A0F4&lМагазин за очки")
                .lore(
                        "&7Покупайте бустеры монет и ценные",
                        "&7предметы за заработанные очки.",
                        "",
                        "&f▶ &eЛКМ &7— открыть магазин"
                ).build();
        setItem(15, shopIcon, e -> {
            new ShopMenu(plugin, player).open();
        });

        // 4. Close button
        ItemStack closeIcon = new ItemBuilder(Material.BARRIER).name("&cЗакрыть").build();
        setItem(22, closeIcon, e -> {
            Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
        });
    }
}
