package com.ejyqyl.atomduels.gui;

import com.ejyqyl.atomduels.AtomDuels;
import com.ejyqyl.atomduels.kit.Kit;
import com.ejyqyl.atomduels.rules.RuleSet;
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
 * Duel conditions & challenge customization menu ("Условия боя").
 * Matches user screenshots with Kit, Mode, Rounds, Stakes, and Arena pickers.
 * Authored by ejyqyl.
 */
public class DuelSettingsMenu implements AtomMenu {

    private final AtomDuels plugin;
    private final Player challenger;
    private final Player target; // null if open challenge
    private final RuleSet ruleSet;
    private final Inventory inventory;

    public DuelSettingsMenu(AtomDuels plugin, Player challenger, Player target, RuleSet ruleSet) {
        this.plugin = plugin;
        this.challenger = challenger;
        this.target = target;
        this.ruleSet = ruleSet != null ? ruleSet : new RuleSet();
        this.inventory = Bukkit.createInventory(this, 36, ColorUtil.parse("<gradient:#00B5FD:#7670E5>Условия боя</gradient>"));
        build();
    }

    public void build() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        // Kit selection
        Kit kit = plugin.getKitManager().getKit(ruleSet.getKitName());
        Material kitIcon = kit != null ? kit.getIcon() : Material.DIAMOND_SWORD;
        inventory.setItem(10, new ItemBuilder(kitIcon)
                .name("<gradient:#00B5FD:#7670E5>Выбранный кит</gradient>")
                .lore("&7Текущий: &e" + ruleSet.getKitName(),
                      " ",
                      "&e▶ ЛКМ: следующий кит",
                      "&6▶ ПКМ: предыдущий кит")
                .hideFlags()
                .build());

        // Duel Mode (Ranked, Casual, Own Items)
        inventory.setItem(12, new ItemBuilder(Material.NETHER_STAR)
                .name("<gradient:#FFD700:#FFAA00>Режим дуэли</gradient>")
                .lore("&7Текущий: &e" + ruleSet.getMode().getDisplayName(),
                      "&7Изменение рейтинга: " + (ruleSet.getMode().isChangesElo() ? "&aДа" : "&cНет"),
                      "&7Выпадение инвентаря: " + (ruleSet.getMode().isDropInventoryOnDeath() ? "&cДа" : "&aНет"),
                      " ",
                      "&e▶ Нажмите для переключения")
                .hideFlags()
                .build());

        // Rounds (Bo1, Bo3, Bo5)
        inventory.setItem(14, new ItemBuilder(Material.CLOCK)
                .name("<gradient:#00FF88:#00B5FD>Количество раундов</gradient>")
                .lore("&7Формат: &eBo" + ruleSet.getMaxRounds() + " &7(до &b" + ruleSet.getRoundsToWin() + " &7побед)",
                      " ",
                      "&e▶ Нажмите для переключения (Bo1 / Bo3 / Bo5)")
                .hideFlags()
                .build());

        // Arena Picker
        String arenaDisp = ruleSet.getArenaName() == null ? "Любая свободная" : ruleSet.getArenaName();
        inventory.setItem(16, new ItemBuilder(Material.MAP)
                .name("<gradient:#A335EE:#7670E5>Арена</gradient>")
                .lore("&7Локация: &e" + arenaDisp,
                      " ",
                      "&e▶ Нажмите для выбора арены")
                .hideFlags()
                .build());

        // Stake / Bet
        inventory.setItem(20, new ItemBuilder(Material.GOLD_INGOT)
                .name("<gradient:#FFAA00:#FFD700>Ставка на бой</gradient>")
                .lore("&7Сумма: &6" + String.format("%.2f", ruleSet.getBetAmount()) + " $",
                      "&7Комиссия при победе: &c" + plugin.getBetManager().getFeePercent() + "%",
                      " ",
                      "&e▶ ЛКМ: +100$ | ПКМ: Сбросить")
                .hideFlags()
                .build());

        // Rules toggle
        inventory.setItem(24, new ItemBuilder(Material.ANVIL)
                .name("<gradient:#FF5555:#FFAA00>Правила боя</gradient>")
                .lore("&7Настройка разрешённых предметов,",
                      "&7щитов, луков, зелий и блоков.",
                      " ",
                      "&e▶ Нажмите для настройки правил")
                .hideFlags()
                .build());

        // Confirm & Send
        String sendName = target != null ? "<gradient:#00FF88:#00B5FD>Бросить вызов игроку " + target.getName() + "</gradient>" : "<gradient:#00FF88:#00B5FD>Создать открытый вызов</gradient>";
        inventory.setItem(31, new ItemBuilder(Material.LIME_CONCRETE)
                .name(sendName)
                .lore("&7Нажмите, чтобы отправить вызов с",
                      "&7выбранными условиями боя.",
                      " ",
                      "&a▶ Нажмите для подтверждения")
                .hideFlags()
                .build());
    }

    public void open() {
        challenger.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        List<Kit> kits = new ArrayList<>(plugin.getKitManager().getKits());

        switch (slot) {
            case 10 -> {
                // Cycle kits
                if (!kits.isEmpty()) {
                    int currentIndex = 0;
                    for (int i = 0; i < kits.size(); i++) {
                        if (kits.get(i).getName().equalsIgnoreCase(ruleSet.getKitName())) {
                            currentIndex = i;
                            break;
                        }
                    }
                    int nextIndex = event.isRightClick() ? (currentIndex - 1 + kits.size()) % kits.size() : (currentIndex + 1) % kits.size();
                    ruleSet.setKitName(kits.get(nextIndex).getName());
                    build();
                }
            }
            case 12 -> {
                // Cycle mode
                RuleSet.DuelMode[] modes = RuleSet.DuelMode.values();
                int nextMode = (ruleSet.getMode().ordinal() + 1) % modes.length;
                ruleSet.setMode(modes[nextMode]);
                build();
            }
            case 14 -> {
                // Cycle Bo1 -> Bo3 -> Bo5 -> Bo1
                int currentRounds = ruleSet.getMaxRounds();
                if (currentRounds == 1) ruleSet.setMaxRounds(3);
                else if (currentRounds == 3) ruleSet.setMaxRounds(5);
                else ruleSet.setMaxRounds(1);
                build();
            }
            case 16 -> new ArenaSelectMenu(plugin, challenger, this).open();
            case 20 -> {
                if (event.isRightClick()) {
                    ruleSet.setBetAmount(0.0);
                } else {
                    ruleSet.setBetAmount(ruleSet.getBetAmount() + 100.0);
                }
                build();
            }
            case 24 -> new RulesSubMenu(plugin, challenger, this, ruleSet).open();
            case 31 -> {
                challenger.closeInventory();
                if (target != null) {
                    plugin.getDuelManager().sendChallenge(challenger, target, ruleSet);
                } else {
                    plugin.getDuelManager().createOpenChallenge(challenger, ruleSet);
                }
            }
        }
    }

    public RuleSet getRuleSet() {
        return ruleSet;
    }
}
