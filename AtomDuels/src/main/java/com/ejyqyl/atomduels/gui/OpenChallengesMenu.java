package com.ejyqyl.atomduels.gui;

import com.ejyqyl.atomduels.AtomDuels;
import com.ejyqyl.atomduels.duel.DuelManager;
import com.ejyqyl.atomduels.rules.RuleSet;
import com.ejyqyl.atomduels.util.ColorUtil;
import com.ejyqyl.atomduels.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public board of open duels with click to join or create.
 * Authored by ejyqyl.
 */
public class OpenChallengesMenu implements AtomMenu {

    private final AtomDuels plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<DuelManager.PendingOpenChallenge> challengeList = new ArrayList<>();

    public OpenChallengesMenu(AtomDuels plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 36, ColorUtil.parse("<gradient:#00B5FD:#7670E5>Открытые вызовы</gradient>"));
        build();
    }

    private void build() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        challengeList.clear();
        challengeList.addAll(plugin.getDuelManager().getOpenChallenges().values());

        int slot = 0;
        for (DuelManager.PendingOpenChallenge challenge : challengeList) {
            if (slot >= 27) break;

            OfflinePlayer creator = Bukkit.getOfflinePlayer(challenge.creator());
            String name = creator.getName() != null ? creator.getName() : "Игрок";
            RuleSet rs = challenge.ruleSet();

            inventory.setItem(slot, new ItemBuilder(Material.PAPER)
                    .name("<#00B5FD>Вызов от &f" + name)
                    .lore("&7Режим: &b" + rs.getMode().getDisplayName(),
                          "&7Кит: &a" + rs.getKitName(),
                          "&7Раундов: &eBo" + rs.getMaxRounds(),
                          "&7Ставка: &6" + String.format("%.2f", rs.getBetAmount()) + " $",
                          "",
                          "&e▶ Нажмите, чтобы принять бой!")
                    .build());
            slot++;
        }

        // Create new challenge button
        inventory.setItem(31, new ItemBuilder(Material.EMERALD)
                .name("<#00FF88>Создать свой открытый вызов")
                .lore("&7Настройте условия и выставите",
                      "&7свой вызов на всеобщее обозрение.",
                      "",
                      "&a▶ Нажмите для настройки")
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
            new DuelSettingsMenu(plugin, player, null, new RuleSet()).open();
            return;
        }

        if (slot >= 0 && slot < challengeList.size()) {
            DuelManager.PendingOpenChallenge challenge = challengeList.get(slot);
            if (challenge.creator().equals(player.getUniqueId())) {
                plugin.getDuelManager().removeOpenChallenge(player.getUniqueId());
                plugin.getMessageManager().sendRawMessage(player, "{prefix} &cВы сняли свой открытый вызов.");
                build();
                return;
            }

            Player challenger = Bukkit.getPlayer(challenge.creator());
            if (challenger != null && challenger.isOnline()) {
                plugin.getDuelManager().removeOpenChallenge(challenge.creator());
                player.closeInventory();
                plugin.getDuelManager().startMatch(challenger, player, challenge.ruleSet(), null);
            }
        }
    }
}
