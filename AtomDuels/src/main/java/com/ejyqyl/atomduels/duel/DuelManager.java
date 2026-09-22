package com.ejyqyl.atomduels.duel;

import com.ejyqyl.atomduels.api.event.DuelEndEvent;
import com.ejyqyl.atomduels.api.event.DuelRoundEndEvent;
import com.ejyqyl.atomduels.api.event.DuelStartEvent;
import com.ejyqyl.atomduels.arena.Arena;
import com.ejyqyl.atomduels.arena.ArenaManager;
import com.ejyqyl.atomduels.bot.BotDifficulty;
import com.ejyqyl.atomduels.bot.BotManager;
import com.ejyqyl.atomduels.bot.DuelBot;
import com.ejyqyl.atomduels.config.ConfigManager;
import com.ejyqyl.atomduels.config.MessageManager;
import com.ejyqyl.atomduels.data.BetManager;
import com.ejyqyl.atomduels.data.DatabaseManager;
import com.ejyqyl.atomduels.data.PlayerData;
import com.ejyqyl.atomduels.data.StatsManager;
import com.ejyqyl.atomduels.elo.EloCalculator;
import com.ejyqyl.atomduels.kit.Kit;
import com.ejyqyl.atomduels.kit.KitManager;
import com.ejyqyl.atomduels.rules.RuleSet;
import com.ejyqyl.atomduels.util.ColorUtil;
import com.ejyqyl.atomduels.util.InventorySnapshot;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core duel lifecycle coordinator.
 * Manages challenges, arena allocations, countdowns, rounds, bets, Elo adjustments, and cleanups.
 * Authored by ejyqyl.
 */
public class DuelManager {

    public record PendingChallenge(UUID sender, UUID target, RuleSet ruleSet, long createdAt) {
        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > 60_000L;
        }
    }

    public record PendingOpenChallenge(UUID creator, RuleSet ruleSet, long createdAt) {
        public boolean isExpired(int seconds) {
            return System.currentTimeMillis() - createdAt > (seconds * 1000L);
        }
    }

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final ArenaManager arenaManager;
    private final KitManager kitManager;
    private final StatsManager statsManager;
    private final BetManager betManager;
    private final BotManager botManager;
    private final DatabaseManager dbManager;

    private final Map<UUID, ActiveDuel> activeDuels = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToDuel = new ConcurrentHashMap<>();
    private final Map<UUID, PendingChallenge> pendingChallenges = new ConcurrentHashMap<>();
    private final Map<UUID, PendingOpenChallenge> openChallenges = new ConcurrentHashMap<>();
    private final Map<UUID, Location> preDuelLocations = new ConcurrentHashMap<>();

    public DuelManager(Plugin plugin, ConfigManager configManager, MessageManager messageManager,
                       ArenaManager arenaManager, KitManager kitManager, StatsManager statsManager,
                       BetManager betManager, BotManager botManager, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.arenaManager = arenaManager;
        this.kitManager = kitManager;
        this.statsManager = statsManager;
        this.betManager = betManager;
        this.botManager = botManager;
        this.dbManager = dbManager;
    }

    public boolean isInDuel(UUID playerUuid) {
        return playerToDuel.containsKey(playerUuid);
    }

    public ActiveDuel getDuel(UUID playerUuid) {
        UUID duelId = playerToDuel.get(playerUuid);
        return duelId != null ? activeDuels.get(duelId) : null;
    }

    public ActiveDuel getDuelById(UUID duelId) {
        return activeDuels.get(duelId);
    }

    public Collection<ActiveDuel> getActiveDuels() {
        return Collections.unmodifiableCollection(activeDuels.values());
    }

    public Map<UUID, PendingOpenChallenge> getOpenChallenges() {
        return Collections.unmodifiableMap(openChallenges);
    }

    /**
     * Creates a challenge to another player.
     */
    public boolean sendChallenge(Player sender, Player target, RuleSet ruleSet) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            messageManager.sendMessage(sender, "challenge.cannot-challenge-self");
            return false;
        }
        if (isInDuel(sender.getUniqueId()) || isInDuel(target.getUniqueId())) {
            messageManager.sendMessage(sender, "challenge.already-in-duel");
            return false;
        }

        double bet = ruleSet.getBetAmount();
        if (bet > 0) {
            PlayerData data = statsManager.getPlayerData(sender);
            if (!betManager.holdBet(UUID.randomUUID(), sender.getUniqueId(), null, bet)) {
                Map<String, String> p = Map.of("BET", String.format("%.2f", bet));
                messageManager.sendMessage(sender, "challenge.insufficient-funds", p);
                return false;
            } else {
                // Refund pre-check hold
                betManager.refundBet(null, sender.getUniqueId(), null, bet, "PRE_CHECK_REFUND");
            }
        }

        pendingChallenges.put(target.getUniqueId(), new PendingChallenge(sender.getUniqueId(), target.getUniqueId(), ruleSet, System.currentTimeMillis()));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("TARGET", target.getName());
        placeholders.put("SENDER", sender.getName());
        placeholders.put("MODE", ruleSet.getMode().getDisplayName());
        placeholders.put("KIT", ruleSet.getKitName());
        placeholders.put("BET", String.format("%.2f", bet));
        placeholders.put("ROUNDS", String.valueOf(ruleSet.getMaxRounds()));

        messageManager.sendMessage(sender, "challenge.sent", placeholders);
        messageManager.sendMessage(target, "challenge.received", placeholders);
        return true;
    }

    public void createOpenChallenge(Player player, RuleSet ruleSet) {
        if (isInDuel(player.getUniqueId())) {
            messageManager.sendMessage(player, "challenge.already-in-duel");
            return;
        }
        openChallenges.put(player.getUniqueId(), new PendingOpenChallenge(player.getUniqueId(), ruleSet, System.currentTimeMillis()));
        messageManager.sendRawMessage(player, "{prefix} &aОткрытый вызов создан! Игроки могут принять его в &e/duels open&a.");
    }

    public void removeOpenChallenge(UUID playerUuid) {
        openChallenges.remove(playerUuid);
    }

    public boolean acceptChallenge(Player target, UUID challengerUuid) {
        PendingChallenge challenge = pendingChallenges.get(target.getUniqueId());
        if (challenge == null || challenge.isExpired() || (challengerUuid != null && !challenge.sender().equals(challengerUuid))) {
            messageManager.sendMessage(target, "challenge.expired");
            pendingChallenges.remove(target.getUniqueId());
            return false;
        }

        Player sender = Bukkit.getPlayer(challenge.sender());
        if (sender == null || !sender.isOnline()) {
            messageManager.sendMessage(target, "player-not-found", Map.of("TARGET", "Challenger"));
            pendingChallenges.remove(target.getUniqueId());
            return false;
        }

        pendingChallenges.remove(target.getUniqueId());
        return startMatch(sender, target, challenge.ruleSet(), null);
    }

    public void denyChallenge(Player target) {
        PendingChallenge challenge = pendingChallenges.remove(target.getUniqueId());
        if (challenge != null) {
            messageManager.sendMessage(target, "challenge.declined");
            Player sender = Bukkit.getPlayer(challenge.sender());
            if (sender != null && sender.isOnline()) {
                messageManager.sendRawMessage(sender, "{prefix} &cИгрок &e" + target.getName() + " &cотклонил ваш вызов.");
            }
        }
    }

    /**
     * Starts a duel match against a PvP Combat Bot.
     */
    public boolean startBotDuel(Player player, BotDifficulty difficulty, RuleSet ruleSet) {
        if (isInDuel(player.getUniqueId())) {
            messageManager.sendMessage(player, "challenge.already-in-duel");
            return false;
        }

        Arena arena = resolveArena(ruleSet.getArenaName());
        if (arena == null) {
            messageManager.sendRawMessage(player, "{prefix} &cНет доступных свободных арен!");
            return false;
        }

        Kit kit = kitManager.getKit(ruleSet.getKitName());
        if (kit == null) kit = kitManager.getKit("Classic");

        // Lock arena
        arena.setInUse(true);

        ActiveDuel duel = new ActiveDuel(player.getUniqueId(), null, arena, kit, ruleSet);
        arena.setCurrentDuelId(duel.getDuelId());
        activeDuels.put(duel.getDuelId(), duel);
        playerToDuel.put(player.getUniqueId(), duel.getDuelId());

        preDuelLocations.put(player.getUniqueId(), player.getLocation().clone());
        duel.setSnapshotP1(new InventorySnapshot(player));

        // Teleport player
        player.teleport(arena.getSpawn1());
        duel.getKit().apply(player);

        // Spawn bot
        DuelBot bot = botManager.spawnBot(player, arena.getSpawn2(), kit, difficulty);
        duel.setBot(bot);

        // Database active state
        dbManager.recordActiveDuel(duel.getDuelId(), player.getUniqueId(), null, ruleSet.getBetAmount(), ruleSet.getMode().name(), kit.getName(), arena.getName());

        startCountdown(duel, player, null);
        return true;
    }

    /**
     * Starts a match between two players.
     */
    public boolean startMatch(Player p1, Player p2, RuleSet ruleSet, Arena targetArena) {
        Arena arena = targetArena != null && !targetArena.isInUse() ? targetArena : resolveArena(ruleSet.getArenaName());
        if (arena == null) {
            messageManager.sendRawMessage(p1, "{prefix} &cНет доступных свободных арен!");
            messageManager.sendRawMessage(p2, "{prefix} &cНет доступных свободных арен!");
            return false;
        }

        double bet = ruleSet.getBetAmount();
        UUID duelId = UUID.randomUUID();

        // Lock escrow bets
        if (bet > 0) {
            boolean locked = betManager.holdBet(duelId, p1.getUniqueId(), p2.getUniqueId(), bet);
            if (!locked) {
                messageManager.sendMessage(p1, "challenge.insufficient-funds", Map.of("BET", String.valueOf(bet)));
                messageManager.sendMessage(p2, "challenge.insufficient-funds", Map.of("BET", String.valueOf(bet)));
                return false;
            }
        }

        arena.setInUse(true);
        Kit kit = kitManager.getKit(ruleSet.getKitName());
        if (kit == null) kit = kitManager.getKit("Classic");

        ActiveDuel duel = new ActiveDuel(p1.getUniqueId(), p2.getUniqueId(), arena, kit, ruleSet);
        arena.setCurrentDuelId(duel.getDuelId());
        activeDuels.put(duel.getDuelId(), duel);
        playerToDuel.put(p1.getUniqueId(), duel.getDuelId());
        playerToDuel.put(p2.getUniqueId(), duel.getDuelId());

        preDuelLocations.put(p1.getUniqueId(), p1.getLocation().clone());
        preDuelLocations.put(p2.getUniqueId(), p2.getLocation().clone());

        duel.setSnapshotP1(new InventorySnapshot(p1));
        duel.setSnapshotP2(new InventorySnapshot(p2));

        // Teleport
        p1.teleport(arena.getSpawn1());
        p2.teleport(arena.getSpawn2());

        if (ruleSet.getMode() != RuleSet.DuelMode.OWN_ITEMS) {
            kit.apply(p1);
            kit.apply(p2);
        }

        dbManager.recordActiveDuel(duel.getDuelId(), p1.getUniqueId(), p2.getUniqueId(), bet, ruleSet.getMode().name(), kit.getName(), arena.getName());
        Bukkit.getPluginManager().callEvent(new DuelStartEvent(duel.getDuelId(), p1, p2, false, arena.getName(), kit.getName()));

        startCountdown(duel, p1, p2);
        return true;
    }

    private void startCountdown(ActiveDuel duel, Player p1, Player p2) {
        duel.setState(DuelState.COUNTDOWN);
        int totalSeconds = configManager.getCountdownSeconds();

        new BukkitRunnable() {
            int remaining = totalSeconds;

            @Override
            public void run() {
                if (duel.getState() == DuelState.ENDED) {
                    cancel();
                    return;
                }

                if (remaining > 0) {
                    String titleStr = "<gradient:#00B5FD:#7670E5>" + remaining + "</gradient>";
                    Title title = Title.title(ColorUtil.parse(titleStr), ColorUtil.parse("&7Приготовьтесь к бою!"),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(200)));

                    if (p1 != null && p1.isOnline()) {
                        p1.showTitle(title);
                        p1.playSound(p1.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                    if (p2 != null && p2.isOnline()) {
                        p2.showTitle(title);
                        p2.playSound(p2.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                    remaining--;
                } else {
                    duel.setState(DuelState.IN_FIGHT);
                    Title title = Title.title(ColorUtil.parse("<gradient:#00FF88:#00B5FD>В БОЙ!</gradient>"),
                            ColorUtil.parse("&7Да победит сильнейший!"),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(300)));

                    if (p1 != null && p1.isOnline()) {
                        p1.showTitle(title);
                        p1.playSound(p1.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.2f);
                    }
                    if (p2 != null && p2.isOnline()) {
                        p2.showTitle(title);
                        p2.playSound(p2.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.2f);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Handles death of a player in a duel round.
     */
    public void handleDeath(Player victim) {
        ActiveDuel duel = getDuel(victim.getUniqueId());
        if (duel == null || duel.getState() != DuelState.IN_FIGHT) {
            return;
        }

        UUID winnerUuid = duel.getOpponent(victim.getUniqueId());
        Player winner = winnerUuid != null ? Bukkit.getPlayer(winnerUuid) : null;

        // Multi-round tracking
        if (victim.getUniqueId().equals(duel.getPlayer1())) {
            duel.incrementP2RoundWins();
        } else {
            duel.incrementP1RoundWins();
        }

        Bukkit.getPluginManager().callEvent(new DuelRoundEndEvent(duel.getDuelId(), duel.getCurrentRound(), winner));

        int neededWins = duel.getRuleSet().getRoundsToWin();
        boolean finished = duel.getP1RoundWins() >= neededWins || duel.getP2RoundWins() >= neededWins;

        if (finished) {
            finishDuel(duel, winnerUuid, false);
        } else {
            // Next round
            duel.setState(DuelState.ROUND_RESET);
            duel.startNextRound();
            duel.getBlockTracker().rollback();

            String score = duel.getP1RoundWins() + " : " + duel.getP2RoundWins();
            Map<String, String> p = Map.of("ROUND", String.valueOf(duel.getCurrentRound() - 1), "WINNER", winner != null ? winner.getName() : "Бот", "SCORE", score);

            Player p1 = Bukkit.getPlayer(duel.getPlayer1());
            Player p2 = duel.getPlayer2() != null ? Bukkit.getPlayer(duel.getPlayer2()) : null;

            if (p1 != null) messageManager.sendMessage(p1, "match.round-win", p);
            if (p2 != null) messageManager.sendMessage(p2, "match.round-win", p);

            // Reset positions and kits
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p1 != null && p1.isOnline()) {
                    p1.teleport(duel.getArena().getSpawn1());
                    duel.getKit().apply(p1);
                }
                if (p2 != null && p2.isOnline()) {
                    p2.teleport(duel.getArena().getSpawn2());
                    duel.getKit().apply(p2);
                }
                startCountdown(duel, p1, p2);
            }, 40L);
        }
    }

    public void handleBotDeath(DuelBot bot) {
        for (ActiveDuel duel : activeDuels.values()) {
            if (duel.getBot() != null && duel.getBot().getBotId().equals(bot.getBotId())) {
                Player p1 = Bukkit.getPlayer(duel.getPlayer1());
                duel.incrementP1RoundWins();
                int needed = duel.getRuleSet().getRoundsToWin();
                if (duel.getP1RoundWins() >= needed) {
                    finishDuel(duel, duel.getPlayer1(), false);
                } else {
                    duel.setState(DuelState.ROUND_RESET);
                    duel.startNextRound();
                    duel.getBlockTracker().rollback();
                    bot.remove();

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (p1 != null && p1.isOnline()) {
                            p1.teleport(duel.getArena().getSpawn1());
                            duel.getKit().apply(p1);
                            DuelBot newBot = botManager.spawnBot(p1, duel.getArena().getSpawn2(), duel.getKit(), bot.getProfile().getDifficulty());
                            duel.setBot(newBot);
                            startCountdown(duel, p1, null);
                        }
                    }, 40L);
                }
                return;
            }
        }
    }

    public void handleQuit(Player player) {
        ActiveDuel duel = getDuel(player.getUniqueId());
        if (duel == null) return;

        UUID winnerUuid = duel.getOpponent(player.getUniqueId());
        messageManager.sendMessage(player, "match.combat-log", Map.of("PLAYER", player.getName()));
        finishDuel(duel, winnerUuid, false);
    }

    public void handleSurrender(Player player) {
        ActiveDuel duel = getDuel(player.getUniqueId());
        if (duel == null) return;

        UUID winnerUuid = duel.getOpponent(player.getUniqueId());
        messageManager.sendRawMessage(player, "{prefix} &cВы сдались в дуэли!");
        finishDuel(duel, winnerUuid, false);
    }

    public void finishDuel(ActiveDuel duel, UUID winnerUuid, boolean isDraw) {
        duel.setState(DuelState.ENDED);
        activeDuels.remove(duel.getDuelId());

        Player p1 = Bukkit.getPlayer(duel.getPlayer1());
        Player p2 = duel.getPlayer2() != null ? Bukkit.getPlayer(duel.getPlayer2()) : null;

        playerToDuel.remove(duel.getPlayer1());
        if (duel.getPlayer2() != null) {
            playerToDuel.remove(duel.getPlayer2());
        }

        // 1. Rollback arena blocks
        duel.getBlockTracker().rollback();
        duel.getArena().setInUse(false);

        // 2. Remove bots
        if (duel.getBot() != null) {
            botManager.removeBot(duel.getBot().getBotId());
        }

        // 3. Delete from active_duels database table
        dbManager.removeActiveDuel(duel.getDuelId());

        Player winner = winnerUuid != null ? Bukkit.getPlayer(winnerUuid) : null;
        Player loser = winnerUuid != null ? (winnerUuid.equals(duel.getPlayer1()) ? p2 : p1) : null;
        UUID loserUuid = duel.getOpponent(winnerUuid);

        // 4. Elo and Stats
        String eloChangeStr = "0";
        double bet = duel.getRuleSet().getBetAmount();

        if (isDraw) {
            betManager.refundBet(duel.getDuelId(), duel.getPlayer1(), duel.getPlayer2(), bet, "DRAW");
            if (p1 != null) {
                PlayerData d1 = statsManager.getPlayerData(p1);
                d1.incrementDraws();
                statsManager.savePlayerData(p1.getUniqueId());
            }
            if (p2 != null) {
                PlayerData d2 = statsManager.getPlayerData(p2);
                d2.incrementDraws();
                statsManager.savePlayerData(p2.getUniqueId());
            }
        } else if (!duel.isBotMatch() && duel.getRuleSet().getMode().isChangesElo() && p1 != null && p2 != null) {
            PlayerData data1 = statsManager.getPlayerData(p1);
            PlayerData data2 = statsManager.getPlayerData(p2);

            double scoreA = winnerUuid.equals(p1.getUniqueId()) ? 1.0 : 0.0;
            EloCalculator.EloResult res = EloCalculator.calculate(data1.getElo(), data2.getElo(), data1.getTotalMatches(), data2.getTotalMatches(), scoreA);

            data1.setElo(res.newRatingA());
            data2.setElo(res.newRatingB());

            if (winnerUuid.equals(p1.getUniqueId())) {
                data1.incrementWins();
                data2.incrementLosses();
                eloChangeStr = "+" + res.deltaA() + " (" + res.newRatingA() + ")";
            } else {
                data2.incrementWins();
                data1.incrementLosses();
                eloChangeStr = "+" + res.deltaB() + " (" + res.newRatingB() + ")";
            }

            statsManager.savePlayerData(p1.getUniqueId());
            statsManager.savePlayerData(p2.getUniqueId());
        } else {
            // Unranked / Bot win
            if (winner != null) {
                PlayerData wData = statsManager.getPlayerData(winner);
                wData.incrementWins();
                statsManager.savePlayerData(winner.getUniqueId());
            }
            if (loser != null) {
                PlayerData lData = statsManager.getPlayerData(loser);
                lData.incrementLosses();
                statsManager.savePlayerData(loser.getUniqueId());
            }
        }

        // 5. Betting payout
        double totalPot = bet * 2.0;
        double fee = totalPot * (betManager.getFeePercent() / 100.0);
        double prize = totalPot - fee;

        if (!isDraw && bet > 0) {
            betManager.payoutWinner(duel.getDuelId(), winnerUuid, loserUuid, bet);
        }

        // 6. Victory banner announcement
        Map<String, String> bannerParams = new HashMap<>();
        bannerParams.put("WINNER", winner != null ? winner.getName() : "Бот");
        bannerParams.put("HP", winner != null ? String.format("%.1f", winner.getHealth()) : "20.0");
        bannerParams.put("PRIZE", String.format("%.2f", prize));
        bannerParams.put("FEE", String.format("%.2f", fee));
        bannerParams.put("ELO_CHANGE", eloChangeStr);

        if (p1 != null && p1.isOnline()) {
            if (isDraw) messageManager.sendMessage(p1, "match.draw-banner");
            else messageManager.sendMessage(p1, "match.winner-banner", bannerParams);
        }
        if (p2 != null && p2.isOnline()) {
            if (isDraw) messageManager.sendMessage(p2, "match.draw-banner");
            else messageManager.sendMessage(p2, "match.winner-banner", bannerParams);
        }

        // 7. Inventory restoration and overflow handling
        restorePlayerInventory(p1, duel.getSnapshotP1());
        restorePlayerInventory(p2, duel.getSnapshotP2());

        // 8. Teleport back
        Location loc1 = preDuelLocations.remove(duel.getPlayer1());
        if (p1 != null && p1.isOnline() && loc1 != null) {
            p1.teleport(loc1);
        }
        if (duel.getPlayer2() != null) {
            Location loc2 = preDuelLocations.remove(duel.getPlayer2());
            if (p2 != null && p2.isOnline() && loc2 != null) {
                p2.teleport(loc2);
            }
        }

        Bukkit.getPluginManager().callEvent(new DuelEndEvent(duel.getDuelId(), winner, loser, isDraw, bet));
    }

    private void restorePlayerInventory(Player player, InventorySnapshot snapshot) {
        if (player == null || !player.isOnline() || snapshot == null) return;

        List<ItemStack> overflow = snapshot.restore(player);
        if (!overflow.isEmpty()) {
            PlayerData data = statsManager.getPlayerData(player);
            data.getClaimItems().addAll(overflow);
            statsManager.savePlayerData(player.getUniqueId());
            messageManager.sendRawMessage(player, "{prefix} &eЧасть ваших предметов не поместилась и была отправлена в &6/duels claim&e!");
        }
    }

    private Arena resolveArena(String preferredName) {
        if (preferredName != null) {
            Arena arena = arenaManager.getArena(preferredName);
            if (arena != null && arena.isEnabled() && !arena.isInUse() && arena.isConfigured()) {
                return arena;
            }
        }
        return arenaManager.getRandomAvailableArena();
    }

    public void cleanup() {
        for (ActiveDuel duel : new ArrayList<>(activeDuels.values())) {
            finishDuel(duel, null, true);
        }
        activeDuels.clear();
        playerToDuel.clear();
        botManager.cleanupAll();
    }
}
