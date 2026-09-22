package com.ejyqyl.atomduels.gui;

import com.ejyqyl.atomduels.AtomDuels;
import com.ejyqyl.atomduels.rules.Rule;
import com.ejyqyl.atomduels.rules.RuleSet;
import com.ejyqyl.atomduels.util.ColorUtil;
import com.ejyqyl.atomduels.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Sub-menu for toggling individual combat rules.
 * Authored by ejyqyl.
 */
public class RulesSubMenu implements AtomMenu {

    private final AtomDuels plugin;
    private final Player player;
    private final DuelSettingsMenu parentMenu;
    private final RuleSet ruleSet;
    private final Inventory inventory;

    public RulesSubMenu(AtomDuels plugin, Player player, DuelSettingsMenu parentMenu, RuleSet ruleSet) {
        this.plugin = plugin;
        this.player = player;
        this.parentMenu = parentMenu;
        this.ruleSet = ruleSet;
        this.inventory = Bukkit.createInventory(this, 36, ColorUtil.parse("<gradient:#00B5FD:#7670E5>Настройка правил боя</gradient>"));
        build();
    }

    private void build() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        Rule[] rules = Rule.values();
        for (int i = 0; i < rules.length && i < 27; i++) {
            Rule rule = rules[i];
            boolean enabled = ruleSet.isRuleEnabled(rule);
            String status = enabled ? "&aВКЛЮЧЕНО" : "&cОТКЛЮЧЕНО";

            inventory.setItem(i, new ItemBuilder(rule.getIcon())
                    .name((enabled ? "<#00FF88>" : "<#FF5555>") + rule.getDisplayName())
                    .lore("&7" + rule.getDescription(),
                          "&7Статус: " + status,
                          "",
                          "&e▶ Нажмите для переключения")
                    .build());
        }

        // Back button
        inventory.setItem(31, new ItemBuilder(Material.ARROW)
                .name("<#FFAA00>Назад")
                .lore("&7Вернуться к общим настройкам боя.")
                .build());
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 31) {
            parentMenu.open();
            return;
        }

        Rule[] rules = Rule.values();
        if (slot >= 0 && slot < rules.length && slot < 27) {
            ruleSet.toggleRule(rules[slot]);
            build();
        }
    }
}
