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
import java.util.Map;

/**
 * Matchmaking queue selector for Ranked and Casual duels.
 * Authored by ejyqyl.
 */
public class QueueMenu implements AtomMenu {

    private final AtomDuels plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<Kit> kits = new ArrayList<>();

    public QueueMenu(AtomDuels plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, ColorUtil.parse("<gradient:#00B5FD:#7670E5>Поиск боя (Очередь)</gradient>"));
        build();
    }

    private void build() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        kits.clear();
        kits.addAll(plugin.getKitManager().getKits());

        boolean inQueue = plugin.getQueueManager().isInQueue(player.getUniqueId());

        int slot = 11;
        for (Kit kit : kits) {
            if (slot > 15) break;

            inventory.setItem(slot, new ItemBuilder(kit.getIcon())
                    .name(kit.getDisplayName())
                    .lore("&7Режим: &bРейтинговый (Elo)",
                          "",
                          "&e▶ ЛКМ: Войти в Рейтинговую очередь",
                          "&6▶ ПКМ: Войти в Обычную очередь")
                    .hideFlags()
                    .build());
            slot++;
        }

        if (inQueue) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name("<#FF5555>Покинуть очередь")
                    .lore("&7Нажмите, чтобы отменить поиск соперника.")
                    .hideFlags()
                    .build());
        }
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

        if (slot == 22) {
            plugin.getQueueManager().removeFromQueue(player.getUniqueId());
            plugin.getMessageManager().sendMessage(player, "queue.left");
            player.closeInventory();
            return;
        }

        int index = slot - 11;
        if (index >= 0 && index < kits.size() && index < 5) {
            Kit kit = kits.get(index);
            boolean ranked = !event.isRightClick();
            plugin.getQueueManager().addToQueue(player, kit.getName(), ranked);
            Map<String, String> p = Map.of("MODE", ranked ? "Рейтинговый" : "Обычный", "KIT", kit.getName());
            plugin.getMessageManager().sendMessage(player, "queue.joined", p);
            player.closeInventory();
        }
    }
}
