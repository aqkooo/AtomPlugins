package com.ejyqyl.atompvpbot.gui;

import com.ejyqyl.atompvpbot.ai.BotBehaviorMode;
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
 * Menu to configure continuous training mode and quick toggle settings.
 *
 * @author ejyqyl
 */
public class SettingsMenu extends AtomMenu {

    private final Plugin plugin;
    private final ArenaManager arenaManager;
    private final KitManager kitManager;
    private final FightManager fightManager;
    private final StatsManager statsManager;

    public SettingsMenu(Plugin plugin, Player player, ArenaManager arenaManager, KitManager kitManager,
                        FightManager fightManager, StatsManager statsManager) {
        super(player, 27, "<gradient:#00B5FD:#7670E5>AtomPvPbot</gradient> &8| &7Настройки");
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

        // 1. Continuous Training Toggle (Slot 11)
        List<String> trainLore = new ArrayList<>();
        trainLore.add("&7При включении бой не прекращается");
        trainLore.add("&7после гибели одного из участников:");
        trainLore.add("&7через 3 секунды раунд перезапускается.");
        trainLore.add("");
        trainLore.add(" &7• Состояние: " + (prefs.isContinuousTraining() ? "&aВключено" : "&cВыключено"));
        trainLore.add("");
        trainLore.add("&eНажмите, чтобы переключить");

        Material trainMat = prefs.isContinuousTraining() ? Material.LIME_CONCRETE : Material.RED_CONCRETE;

        ItemStack trainItem = new ItemBuilder(trainMat)
                .name("<gradient:#A8FF78:#78FFD6>🔄 Бесконечная тренировка</gradient>")
                .lore(trainLore)
                .hideFlags()
                .build();

        setItem(11, trainItem, e -> {
            prefs.setContinuousTraining(!prefs.isContinuousTraining());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            initialize(); // Refresh
        });

        // 2. Quick Difficulty Cycle (Slot 13)
        List<String> diffLore = new ArrayList<>();
        diffLore.add("&7Текущий уровень: " + prefs.getDifficulty().getDisplayName());
        diffLore.add("");
        diffLore.add("&eНажмите, чтобы переключить на следующий");

        ItemStack diffItem = new ItemBuilder(Material.TARGET)
                .name("<gradient:#FFFF55:#FFAA00>🎯 Переключить сложность</gradient>")
                .lore(diffLore)
                .hideFlags()
                .build();

        setItem(13, diffItem, e -> {
            BotDifficulty[] vals = BotDifficulty.values();
            int nextIdx = (prefs.getDifficulty().ordinal() + 1) % vals.length;
            prefs.setDifficulty(vals[nextIdx]);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            initialize();
        });

        // 3. Quick Behavior Cycle (Slot 15)
        List<String> modeLore = new ArrayList<>();
        modeLore.add("&7Текущий стиль: " + prefs.getBehaviorMode().getDisplayName());
        modeLore.add("");
        modeLore.add("&eНажмите, чтобы переключить на следующий");

        ItemStack modeItem = new ItemBuilder(Material.BLAZE_POWDER)
                .name("<gradient:#36D1DC:#5B86E5>🤖 Переключить стиль боя</gradient>")
                .lore(modeLore)
                .hideFlags()
                .build();

        setItem(15, modeItem, e -> {
            BotBehaviorMode[] vals = BotBehaviorMode.values();
            int nextIdx = (prefs.getBehaviorMode().ordinal() + 1) % vals.length;
            prefs.setBehaviorMode(vals[nextIdx]);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            initialize();
        });

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
