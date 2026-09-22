package com.ejyqyl.atompvpbot.gui;

import com.ejyqyl.atompvpbot.ai.BotDifficulty;
import com.ejyqyl.atompvpbot.arena.ArenaManager;
import com.ejyqyl.atompvpbot.data.StatsManager;
import com.ejyqyl.atompvpbot.fight.FightManager;
import com.ejyqyl.atompvpbot.kit.KitManager;
import com.ejyqyl.atompvpbot.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu to select bot difficulty.
 *
 * @author ejyqyl
 */
public class DifficultySelectMenu extends AtomMenu {

    private final Plugin plugin;
    private final ArenaManager arenaManager;
    private final KitManager kitManager;
    private final FightManager fightManager;
    private final StatsManager statsManager;

    public DifficultySelectMenu(Plugin plugin, Player player, ArenaManager arenaManager, KitManager kitManager,
                                FightManager fightManager, StatsManager statsManager) {
        super(player, 27, "<gradient:#00B5FD:#7670E5>AtomPvPbot</gradient> &8| &7Выбор сложности");
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.kitManager = kitManager;
        this.fightManager = fightManager;
        this.statsManager = statsManager;
    }

    @Override
    public void initialize() {
        fillBorder(createGlass(Material.GRAY_STAINED_GLASS_PANE));
        fillEmpty(createGlass(Material.BLACK_STAINED_GLASS_PANE));

        PlayerFightPreferences prefs = PlayerFightPreferences.get(player.getUniqueId());

        BotDifficulty[] diffs = BotDifficulty.values();
        int[] slots = {10, 11, 12, 14, 16};

        for (int i = 0; i < diffs.length && i < slots.length; i++) {
            BotDifficulty diff = diffs[i];
            boolean isCurrent = prefs.getDifficulty() == diff;

            Material icon = switch (diff) {
                case EASY -> Material.LIME_DYE;
                case NORMAL -> Material.YELLOW_DYE;
                case HARD -> Material.ORANGE_DYE;
                case EXPERT -> Material.RED_DYE;
                case CUSTOM -> Material.PURPLE_DYE;
            };

            List<String> lore = new ArrayList<>();
            lore.add("&8Характеристики бота:");
            lore.add(" &7• CPS: &f" + diff.getCps());
            lore.add(" &7• Дистанция удара: &f" + diff.getReach() + " бл.");
            lore.add(" &7• Точность аима: &f" + (int)(diff.getAimAccuracy() * 100) + "%");
            lore.add(" &7• Блок щитом: &f" + (int)(diff.getShieldBlockChance() * 100) + "%");
            lore.add(" &7• Реакция: &f" + diff.getReactionMs() + " мс");
            lore.add(" &7• Комбо-цепочки: " + (diff.isSprintResetCombo() ? "&aДа (до " + diff.getComboMaxHits() + ")" : "&cНет"));
            lore.add(" &7• Пробитие щита: " + (diff.isAxeShieldBreak() ? "&aДа" : "&cНет"));
            lore.add(" &7• Булава 1.21: " + (diff.isMaceSmash() ? "&aДа" : "&cНет"));
            lore.add("");
            if (isCurrent) {
                lore.add("&a✔ Выбрано сейчас");
            } else {
                lore.add("&eНажмите, чтобы выбрать");
            }

            ItemStack item = new ItemBuilder(icon)
                    .name(diff.getDisplayName())
                    .lore(lore)
                    .hideFlags()
                    .build();

            setItem(slots[i], item, e -> {
                prefs.setDifficulty(diff);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                new MainBotMenu(plugin, player, arenaManager, kitManager, fightManager, statsManager).open();
            });
        }

        // Back button
        ItemStack back = new ItemBuilder(Material.ARROW)
                .name("&c« Назад в главное меню")
                .hideFlags()
                .build();

        setItem(22, back, e -> {
            new MainBotMenu(plugin, player, arenaManager, kitManager, fightManager, statsManager).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
        });
    }
}
