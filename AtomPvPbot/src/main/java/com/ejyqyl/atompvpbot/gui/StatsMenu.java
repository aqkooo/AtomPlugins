package com.ejyqyl.atompvpbot.gui;

import com.ejyqyl.atompvpbot.arena.ArenaManager;
import com.ejyqyl.atompvpbot.data.PlayerData;
import com.ejyqyl.atompvpbot.data.StatsManager;
import com.ejyqyl.atompvpbot.fight.FightManager;
import com.ejyqyl.atompvpbot.kit.KitManager;
import com.ejyqyl.atompvpbot.util.ColorUtil;
import com.ejyqyl.atompvpbot.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu displaying detailed player combat statistics.
 *
 * @author ejyqyl
 */
public class StatsMenu extends AtomMenu {

    private final Plugin plugin;
    private final ArenaManager arenaManager;
    private final KitManager kitManager;
    private final FightManager fightManager;
    private final StatsManager statsManager;

    public StatsMenu(Plugin plugin, Player player, ArenaManager arenaManager, KitManager kitManager,
                     FightManager fightManager, StatsManager statsManager) {
        super(player, 27, "<gradient:#00B5FD:#7670E5>AtomPvPbot</gradient> &8| &7Статистика игрока");
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

        PlayerData data = statsManager.getPlayerData(player);

        // Center item (Slot 13): Player Skull with stats
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ColorUtil.colorize("<gradient:#00B5FD:#7670E5>📊 Профиль: " + player.getName() + "</gradient>"));

            List<String> lore = new ArrayList<>();
            lore.add("&8Боевые показатели:");
            lore.add(" &7• Всего матчей: &f" + data.getTotalGames());
            lore.add(" &7• Побед: &a" + data.getWins());
            lore.add(" &7• Поражений: &c" + data.getLosses());
            lore.add(" &7• Винрейт: &e" + String.format("%.1f", data.getWinRate()) + "% " + renderProgressBar(data.getWinRate()));
            lore.add(" &7• Текущая серия побед: &6" + data.getWinStreak());
            lore.add(" &7• Лучшая серия побед: &d" + data.getBestStreak());
            meta.setLore(lore.stream().map(ColorUtil::colorize).toList());
            skull.setItemMeta(meta);
        }
        setItem(13, skull);

        // Back button (Slot 22)
        ItemStack back = new ItemBuilder(Material.ARROW)
                .name("&c« Назад в главное меню")
                .hideFlags()
                .build();

        setItem(22, back, e -> {
            new MainBotMenu(plugin, player, arenaManager, kitManager, fightManager, statsManager).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
        });
    }

    private String renderProgressBar(double percent) {
        int totalBars = 10;
        int greenBars = (int) Math.round((percent / 100.0) * totalBars);
        StringBuilder sb = new StringBuilder("&8[&a");
        for (int i = 0; i < greenBars; i++) {
            sb.append("|");
        }
        sb.append("&c");
        for (int i = greenBars; i < totalBars; i++) {
            sb.append("|");
        }
        sb.append("&8]");
        return sb.toString();
    }
}
