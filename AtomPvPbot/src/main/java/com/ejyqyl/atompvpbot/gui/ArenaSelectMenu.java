package com.ejyqyl.atompvpbot.gui;

import com.ejyqyl.atompvpbot.arena.Arena;
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
 * Menu to pick an arena or random matchmaking.
 *
 * @author ejyqyl
 */
public class ArenaSelectMenu extends AtomMenu {

    private final Plugin plugin;
    private final ArenaManager arenaManager;
    private final KitManager kitManager;
    private final FightManager fightManager;
    private final StatsManager statsManager;

    public ArenaSelectMenu(Plugin plugin, Player player, ArenaManager arenaManager, KitManager kitManager,
                           FightManager fightManager, StatsManager statsManager) {
        super(player, 36, "<gradient:#00B5FD:#7670E5>AtomPvPbot</gradient> &8| &7Выбор арены");
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

        // Slot 10: "Любая свободная"
        boolean isRandom = prefs.getArenaName() == null;
        List<String> randomLore = new ArrayList<>();
        randomLore.add("&7Автоматический подбор любой");
        randomLore.add("&7готовой и свободной арены.");
        randomLore.add("");
        if (isRandom) {
            randomLore.add("&a✔ Выбрано сейчас");
        } else {
            randomLore.add("&eНажмите, чтобы выбрать");
        }

        ItemStack randomItem = new ItemBuilder(Material.NETHER_STAR)
                .name("<gradient:#FFD200:#F7971E>✨ Любая свободная</gradient>")
                .lore(randomLore)
                .hideFlags()
                .build();

        setItem(10, randomItem, e -> {
            prefs.setArenaName(null);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            new MainBotMenu(plugin, player, arenaManager, kitManager, fightManager, statsManager).open();
        });

        int slot = 11;
        for (Arena arena : arenaManager.getArenas()) {
            if (slot > 25) break;
            if (slot % 9 == 8) slot += 2; // Skip border

            boolean isCurrent = arena.getName().equalsIgnoreCase(prefs.getArenaName());
            boolean available = arena.isEnabled() && !arena.isInUse() && arena.isConfigured();

            List<String> lore = new ArrayList<>();
            lore.add("&8Информация об арене:");
            lore.add(" &7• Статус: " + (arena.isInUse() ? "&cЗанята" : (arena.isConfigured() ? "&aСвободна" : "&eНе настроена")));
            lore.add(" &7• Включена: " + (arena.isEnabled() ? "&aДа" : "&cНет"));
            if (!arena.getAllowedKits().isEmpty()) {
                lore.add(" &7• Наборы: &b" + String.join(", ", arena.getAllowedKits()));
            }
            lore.add("");
            if (isCurrent) {
                lore.add("&a✔ Выбрана сейчас");
            } else if (available) {
                lore.add("&eНажмите, чтобы выбрать");
            } else {
                lore.add("&cНедоступна для выбора");
            }

            Material icon = available ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;

            ItemStack item = new ItemBuilder(icon)
                    .name("<gradient:#00B5FD:#7670E5>" + arena.getDisplayName() + "</gradient>")
                    .lore(lore)
                    .hideFlags()
                    .build();

            setItem(slot++, item, e -> {
                if (!available) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                    return;
                }
                prefs.setArenaName(arena.getName().toLowerCase());
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                new MainBotMenu(plugin, player, arenaManager, kitManager, fightManager, statsManager).open();
            });
        }

        // Back button
        ItemStack back = new ItemBuilder(Material.ARROW)
                .name("&c« Назад в главное меню")
                .hideFlags()
                .build();

        setItem(31, back, e -> {
            new MainBotMenu(plugin, player, arenaManager, kitManager, fightManager, statsManager).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
        });
    }
}
