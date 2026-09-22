package com.ejyqyl.atompvpbot.gui;

import com.ejyqyl.atompvpbot.ai.BotBehaviorMode;
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
 * Menu to select bot tactical behavior mode.
 *
 * @author ejyqyl
 */
public class BehaviorModeSelectMenu extends AtomMenu {

    private final Plugin plugin;
    private final ArenaManager arenaManager;
    private final KitManager kitManager;
    private final FightManager fightManager;
    private final StatsManager statsManager;

    public BehaviorModeSelectMenu(Plugin plugin, Player player, ArenaManager arenaManager, KitManager kitManager,
                                  FightManager fightManager, StatsManager statsManager) {
        super(player, 27, "<gradient:#00B5FD:#7670E5>AtomPvPbot</gradient> &8| &7Режим поведения");
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

        BotBehaviorMode[] modes = BotBehaviorMode.values();
        int[] slots = {10, 12, 14, 16};

        for (int i = 0; i < modes.length && i < slots.length; i++) {
            BotBehaviorMode mode = modes[i];
            boolean isCurrent = prefs.getBehaviorMode() == mode;

            Material icon = switch (mode) {
                case AGGRESSIVE -> Material.REDSTONE;
                case DEFENSIVE -> Material.SHIELD;
                case STRAFE -> Material.FEATHER;
                case BALANCED -> Material.IRON_SWORD;
            };

            List<String> lore = new ArrayList<>();
            lore.add("&8Тактика поведения:");
            lore.add(" &7" + mode.getDescription());
            lore.add("");
            lore.add(" &7• Множитель скорости: &f" + mode.getSpeedMultiplier() + "x");
            lore.add(" &7• Тактическое отступление: " + (mode.isRetreatOnLowHp() ? "&aДа" : "&cНет"));
            lore.add("");
            if (isCurrent) {
                lore.add("&a✔ Выбрано сейчас");
            } else {
                lore.add("&eНажмите, чтобы выбрать");
            }

            ItemStack item = new ItemBuilder(icon)
                    .name(mode.getDisplayName())
                    .lore(lore)
                    .hideFlags()
                    .build();

            setItem(slots[i], item, e -> {
                prefs.setBehaviorMode(mode);
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
