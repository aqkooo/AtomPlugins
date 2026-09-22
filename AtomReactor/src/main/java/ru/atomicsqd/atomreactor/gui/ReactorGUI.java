package ru.atomicsqd.atomreactor.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ru.atomicsqd.atomreactor.AtomReactor;
import ru.atomicsqd.atomreactor.config.ReactorLevel;
import ru.atomicsqd.atomreactor.model.Reactor;
import ru.atomicsqd.atomreactor.util.ColorUtil;
import ru.atomicsqd.atomreactor.util.ItemBuilder;

import java.util.List;

/**
 * Reactor management GUI.
 * STRICT REQUIREMENTS:
 * - NO ITALIC FONT ANYWHERE (explicitly stripped from all items and titles)
 * - ONLY HIGH-CONTRAST READABLE GRADIENTS (MiniMessage / HEX)
 */
public class ReactorGUI implements InventoryHolder {

    private final AtomReactor plugin;
    private final Reactor reactor;
    private final Inventory inventory;

    public ReactorGUI(AtomReactor plugin, Reactor reactor) {
        this.plugin = plugin;
        this.reactor = reactor;

        Component title = ColorUtil.component("<gradient:#FF5F6D:#FFC371>&lПанель Управления Реактором</gradient>");
        this.inventory = Bukkit.createInventory(this, 36, title);
        build();
    }

    public static void open(Player player, Reactor reactor, AtomReactor plugin) {
        ReactorGUI gui = new ReactorGUI(plugin, reactor);
        player.openInventory(gui.getInventory());
    }

    public void refresh() {
        build();
    }

    private void build() {
        inventory.clear();

        // Background filler
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("&7")
                .build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        ReactorLevel currentLevel = plugin.getConfigManager().getLevel(reactor.getLevel());
        ReactorLevel nextLevel = plugin.getConfigManager().getNextLevel(reactor.getLevel());
        String cur = plugin.getConfigManager().getCurrencySymbol();

        // 1. Slot 13: Core Info
        String statusStr = reactor.isActive()
                ? "<gradient:#42E695:#3BB78F>● В работе (Активен)</gradient>"
                : "<gradient:#FF512F:#DD2476>● Приостановлен</gradient>";

        ItemStack coreItem = new ItemBuilder(plugin.getConfigManager().getReactorMaterial())
                .name("<gradient:#FF5F6D:#FFC371>&lЯдро Атомного Реактора</gradient>")
                .lore(
                        "<gradient:#56CCF2:#2F80ED>● Владелец:</gradient> &f" + reactor.getOwnerName(),
                        "<gradient:#00C0FF:#42E695>● Уровень системы:</gradient> " + (currentLevel != null ? currentLevel.getName() : "Ур. " + reactor.getLevel()),
                        "<gradient:#42E695:#3BB78F>● Выработка:</gradient> &a+" + (currentLevel != null ? currentLevel.getIncome() : 0.0) + cur + " &7/ " + (currentLevel != null ? currentLevel.getIntervalSeconds() : 30) + "с",
                        "<gradient:#FCE38A:#F38181>● Радиус покрытия:</gradient> &f" + (currentLevel != null ? currentLevel.getRadius() : 10) + " блоков",
                        "<gradient:#F3904F:#3B4371>● Режим распределения:</gradient> &f" + plugin.getConfigManager().getGenerationTarget().name(),
                        "<gradient:#FFE259:#FFA751>● Накоплено в банке:</gradient> &e" + String.format("%.1f", reactor.getStoredBalance()) + cur + " &7/ " + (currentLevel != null ? String.format("%.0f", currentLevel.getMaxStorage()) : "10000") + cur,
                        "",
                        statusStr
                )
                .build();
        inventory.setItem(13, coreItem);

        // 2. Slot 20: Upgrade Button
        if (nextLevel != null) {
            ItemStack upgradeItem = new ItemBuilder(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                    .name("<gradient:#42E695:#3BB78F>&lМодернизировать Реактор</gradient>")
                    .lore(
                            "<gradient:#56CCF2:#2F80ED>● Следующий ранг:</gradient> " + nextLevel.getName(),
                            "<gradient:#00C0FF:#42E695>● Прирост выработки:</gradient> &a+" + nextLevel.getIncome() + cur + " &7(текущая: " + (currentLevel != null ? currentLevel.getIncome() : 0.0) + cur + ")",
                            "<gradient:#FCE38A:#F38181>● Новый интервал:</gradient> &f" + nextLevel.getIntervalSeconds() + " сек.",
                            "<gradient:#FFE259:#FFA751>● Стоимость улучшения:</gradient> &e" + String.format("%.1f", nextLevel.getUpgradeCost()) + cur,
                            "",
                            "<gradient:#FFC371:#FF5F6D>Кликните для покупки улучшения!</gradient>"
                    )
                    .build();
            inventory.setItem(20, upgradeItem);
        } else {
            ItemStack maxItem = new ItemBuilder(Material.BARRIER)
                    .name("<gradient:#FCE38A:#F38181>&lМаксимальный Уровень</gradient>")
                    .lore(
                            "<gradient:#42E695:#3BB78F>Реактор улучшен до максимальной мощности!</gradient>",
                            "<gradient:#56CCF2:#2F80ED>Все системы функционируют на 100%.</gradient>"
                    )
                    .build();
            inventory.setItem(20, maxItem);
        }

        // 3. Slot 22: Collect Stored Bank Button
        ItemStack bankItem = new ItemBuilder(Material.GOLD_INGOT)
                .name("<gradient:#FFE259:#FFA751>&lСобрать Накопленный Доход</gradient>")
                .lore(
                        "<gradient:#42E695:#3BB78F>● Доступно к выводу:</gradient> &a+" + String.format("%.1f", reactor.getStoredBalance()) + cur,
                        "",
                        "<gradient:#56CCF2:#2F80ED>Кликните, чтобы зачислить средства на ваш баланс!</gradient>"
                )
                .build();
        inventory.setItem(22, bankItem);

        // 4. Slot 24: Toggle Active Button
        if (reactor.isActive()) {
            ItemStack toggleItem = new ItemBuilder(Material.LIME_DYE)
                    .name("<gradient:#42E695:#3BB78F>&lПитание: ВКЛЮЧЕНО</gradient>")
                    .lore(
                            "<gradient:#56CCF2:#2F80ED>Реактор вырабатывает энергию и средства.</gradient>",
                            "",
                            "<gradient:#FF5F6D:#FFC371>Кликните, чтобы приостановить работу.</gradient>"
                    )
                    .build();
            inventory.setItem(24, toggleItem);
        } else {
            ItemStack toggleItem = new ItemBuilder(Material.RED_DYE)
                    .name("<gradient:#FF512F:#DD2476>&lПитание: ВЫКЛЮЧЕНО</gradient>")
                    .lore(
                            "<gradient:#F3904F:#3B4371>Выработка энергии временно приостановлена.</gradient>",
                            "",
                            "<gradient:#42E695:#3BB78F>Кликните, чтобы возобновить работу.</gradient>"
                    )
                    .build();
            inventory.setItem(24, toggleItem);
        }
    }

    public Reactor getReactor() {
        return reactor;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
