package com.ejyqyl.atompvpbot.fight;

import com.ejyqyl.atompvpbot.arena.Arena;
import com.ejyqyl.atompvpbot.entity.PvPBotEntity;
import com.ejyqyl.atompvpbot.kit.BotKit;
import com.ejyqyl.atompvpbot.util.ColorUtil;
import com.ejyqyl.atompvpbot.util.InventorySnapshot;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;

/**
 * Manages an active fight session between a player and a PvP bot.
 *
 * @author ejyqyl
 */
public class BotFight {

    public enum State {
        PREPARING,
        COUNTDOWN,
        FIGHTING,
        ROUND_RESET,
        ENDED
    }

    private final Plugin plugin;
    private final Player player;
    private final PvPBotEntity bot;
    private final Arena arena;
    private final BotKit kit;
    private final InventorySnapshot playerSnapshot;

    private State state = State.PREPARING;
    private boolean continuousTraining = true;
    private int playerWins = 0;
    private int botWins = 0;
    private BukkitTask countdownTask;

    public BotFight(Plugin plugin, Player player, PvPBotEntity bot, Arena arena, BotKit kit) {
        this.plugin = plugin;
        this.player = player;
        this.bot = bot;
        this.arena = arena;
        this.kit = kit;
        this.playerSnapshot = new InventorySnapshot(player);
    }

    public void start() {
        if (arena != null) {
            arena.setInUse(true);
        }

        // Teleport player & bot
        if (arena != null && arena.getSpawn1() != null) {
            player.teleport(arena.getSpawn1());
        }
        if (arena != null && arena.getSpawn2() != null) {
            bot.spawn(arena.getSpawn2());
        } else {
            bot.spawn(player.getLocation().add(player.getLocation().getDirection().multiply(4)));
        }

        bot.setFightOpponent(player);
        bot.setArena(arena);

        // Apply Kit
        if (kit != null) {
            kit.applyTo(player);
            bot.applyKit(kit);
        }

        player.setGameMode(GameMode.SURVIVAL);
        startCountdown();
    }

    public void startCountdown() {
        this.state = State.COUNTDOWN;

        if (countdownTask != null) {
            countdownTask.cancel();
        }

        countdownTask = new BukkitRunnable() {
            int count = 3;

            @Override
            public void run() {
                if (!player.isOnline() || state == State.ENDED) {
                    cancel();
                    return;
                }

                if (count > 0) {
                    Title title = Title.title(
                            ColorUtil.parse("<gradient:#00B5FD:#7670E5>" + count + "</gradient>"),
                            ColorUtil.parse("&7Приготовьтесь к бою!"),
                            Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(800), Duration.ofMillis(200))
                    );
                    player.showTitle(title);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f + (3 - count) * 0.2f);
                    count--;
                } else {
                    Title title = Title.title(
                            ColorUtil.parse("<gradient:#55FF55:#00AA00>БОЙ!</gradient>"),
                            ColorUtil.parse("&7Удачи в тренировке!"),
                            Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(800), Duration.ofMillis(200))
                    );
                    player.showTitle(title);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    state = State.FIGHTING;
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void onBotDefeated() {
        playerWins++;
        if (continuousTraining) {
            // Instant continuous reset
            player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &aВы выбили тотем бота! Раунд перезапускается..."));
            resetRound();
        } else {
            endFight(true);
        }
    }

    public void onPlayerDefeated() {
        botWins++;
        if (continuousTraining) {
            player.sendMessage(ColorUtil.parse("<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient> &cБот победил в этом раунде! Раунд перезапускается..."));
            resetRound();
        } else {
            endFight(false);
        }
    }

    private void resetRound() {
        this.state = State.ROUND_RESET;

        // Reset player
        player.setHealth(20.0);
        player.setFoodLevel(20);
        if (kit != null) {
            kit.applyTo(player);
        }
        if (arena != null && arena.getSpawn1() != null) {
            player.teleport(arena.getSpawn1());
        }

        // Reset bot
        bot.reset();
        if (arena != null && arena.getSpawn2() != null) {
            if (bot.getMob() != null) {
                bot.getMob().teleport(arena.getSpawn2());
            }
        }

        startCountdown();
    }

    public void endFight(boolean playerWon) {
        if (state == State.ENDED) return;
        this.state = State.ENDED;

        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        // Restore player
        if (player.isOnline()) {
            playerSnapshot.restore(player);
            if (arena != null && arena.getSpectatorSpawn() != null) {
                player.teleport(arena.getSpectatorSpawn());
            }

            if (playerWon) {
                Title title = Title.title(
                        ColorUtil.parse("<gradient:#55FF55:#00AA00>ПОБЕДА!</gradient>"),
                        ColorUtil.parse("&7Вы победили PvP-бота!"),
                        Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1200), Duration.ofMillis(400))
                );
                player.showTitle(title);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            } else {
                Title title = Title.title(
                        ColorUtil.parse("<gradient:#FF5555:#AA0000>ПОРАЖЕНИЕ</gradient>"),
                        ColorUtil.parse("&7Бот оказался сильнее..."),
                        Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1200), Duration.ofMillis(400))
                );
                player.showTitle(title);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }

        // Clean bot and arena
        bot.despawn();
        if (arena != null) {
            arena.setInUse(false);
        }
    }

    public Player getPlayer() { return player; }
    public PvPBotEntity getBot() { return bot; }
    public Arena getArena() { return arena; }
    public BotKit getKit() { return kit; }
    public State getState() { return state; }
    public boolean isContinuousTraining() { return continuousTraining; }
    public void setContinuousTraining(boolean continuousTraining) { this.continuousTraining = continuousTraining; }
    public int getPlayerWins() { return playerWins; }
    public int getBotWins() { return botWins; }
}
