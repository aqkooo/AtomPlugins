package com.ejyqyl.atomduels.gui;

import com.ejyqyl.atomduels.AtomDuels;
import com.ejyqyl.atomduels.kit.Kit;
import com.ejyqyl.atomduels.util.ColorUtil;
import com.ejyqyl.atomduels.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game administration panel for managing duel kits.
 * Authored by ejyqyl.
 */
public class AdminKitMenu implements AtomMenu {

    private final AtomDuels plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<Kit> kitList = new ArrayList<>();

    public AdminKitMenu(AtomDuels plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 36, ColorUtil.parse("<gradient:#FF5555:#FFAA00>Админ-панель • Киты</gradient>"));
        build();
    }

    private void build() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        kitList.clear();
        kitList.addAll(plugin.getKitManager().getKits());

        int slot = 0;
        for (Kit kit : kitList) {
            if (slot >= 27) break;

            inventory.setItem(slot, new ItemBuilder(kit.getIcon())
                    .name(kit.getDisplayName())
                    .lore("&7Системное имя: &f" + kit.getName(),
                          "",
                          "&e▶ ЛКМ: Получить тестовый кит",
                          "&c▶ Клавиша Q: Удалить кит")
                    .hideFlags()
                    .build());
            slot++;
        }

        // Save current inventory as new kit button
        inventory.setItem(31, new ItemBuilder(Material.ANVIL)
                .name("<#00FF88>Создать кит из инвентаря")
                .lore("&7Сохраняет текущий инвентарь игрока",
                      "&7как новый кит дуэлей.",
                      "",
                      "&a▶ Нажмите для создания (kit_<номер>)")
                .hideFlags()
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
            String newName = "custom_" + (kitList.size() + 1);
            plugin.getKitManager().createKitFromPlayer(newName, player);
            plugin.getMessageManager().sendRawMessage(player, "{prefix} &aКит &e" + newName + " &aуспешно создан из вашего инвентаря!");
            build();
            return;
        }

        if (slot >= 0 && slot < kitList.size()) {
            Kit kit = kitList.get(slot);

            if (event.getClick().isKeyboardClick()) {
                plugin.getKitManager().deleteKit(kit.getName());
                plugin.getMessageManager().sendRawMessage(player, "{prefix} &cКит &e" + kit.getName() + " &cудалён.");
                build();
                return;
            }

            kit.apply(player);
            plugin.getMessageManager().sendRawMessage(player, "{prefix} &aВам выдан кит &e" + kit.getName());
            player.closeInventory();
        }
    }
}
