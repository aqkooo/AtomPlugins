package com.ejyqyl.atompvpbot.gui;

import com.ejyqyl.atompvpbot.arena.ArenaManager;
import com.ejyqyl.atompvpbot.data.StatsManager;
import com.ejyqyl.atompvpbot.fight.FightManager;
import com.ejyqyl.atompvpbot.kit.BotKit;
import com.ejyqyl.atompvpbot.kit.KitManager;
import com.ejyqyl.atompvpbot.util.ColorUtil;
import com.ejyqyl.atompvpbot.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu to browse and select duel kits.
 *
 * @author ejyqyl
 */
public class KitSelectMenu extends AtomMenu {

    private final Plugin plugin;
    private final ArenaManager arenaManager;
    private final KitManager kitManager;
    private final FightManager fightManager;
    private final StatsManager statsManager;

    public KitSelectMenu(Plugin plugin, Player player, ArenaManager arenaManager, KitManager kitManager,
                         FightManager fightManager, StatsManager statsManager) {
        super(player, 36, "<gradient:#00B5FD:#7670E5>AtomPvPbot</gradient> &8| &7Выбор набора");
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

        int slot = 10;
        for (BotKit kit : kitManager.getKits()) {
            if (slot > 25) break;
            if (slot % 9 == 8) slot += 2; // Skip border

            boolean isCurrent = kit.getName().equalsIgnoreCase(prefs.getKitName());

            List<String> lore = new ArrayList<>();
            lore.add("&8Содержимое набора:");
            if (kit.getChestplate() != null) {
                lore.add(" &7• Броня: &f" + formatMaterial(kit.getChestplate().getType()));
            }
            if (kit.getItems().get(0) != null) {
                lore.add(" &7• Оружие: &f" + formatMaterial(kit.getItems().get(0).getType()));
            }
            if (kit.getOffhand() != null) {
                lore.add(" &7• Вторая рука: &f" + formatMaterial(kit.getOffhand().getType()));
            }
            lore.add(" &7• Всего предметов: &f" + kit.getItems().size());
            lore.add("");
            if (isCurrent) {
                lore.add("&a✔ Выбран сейчас");
            } else {
                lore.add("&eНажмите, чтобы выбрать");
            }

            ItemStack item = new ItemBuilder(kit.getIcon() != null ? kit.getIcon() : Material.DIAMOND_SWORD)
                    .name(kit.getDisplayName())
                    .lore(lore)
                    .hideFlags()
                    .build();

            setItem(slot++, item, e -> {
                prefs.setKitName(kit.getName().toLowerCase());
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

    private String formatMaterial(Material mat) {
        if (mat == null) return "Нет";
        String name = mat.name().replace("_", " ").toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
