package glowseller.menu;

import glowseller.Main;
import glowseller.configs.impl.ShopConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MenuManager {
    private final Main plugin;

    public MenuManager(Main plugin) {
        this.plugin = plugin;
    }

    public void openMenuByName(Player player, String name) {
        if (player == null || name == null) return;
        String clean = name.toLowerCase().trim();

        switch (clean) {
            case "main", "menu" -> new MainMenu(plugin, player).open();
            case "sell", "seller", "buyer" -> new SellMenu(plugin, player).open();
            case "shop", "gshop" -> new ShopMenu(plugin, player).open();
            default -> {
                // Check if it matches a category in shop.yml
                ShopConfig.Category cat = plugin.getShopConfig().getCategory(clean);
                if (cat != null) {
                    new ShopCategoryMenu(plugin, player, cat).open();
                } else {
                    new MainMenu(plugin, player).open();
                }
            }
        }
    }

    public void closeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof AbstractMenu) {
                player.closeInventory();
            }
        }
    }
}
