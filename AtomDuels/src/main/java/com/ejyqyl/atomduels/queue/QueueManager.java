package com.ejyqyl.atomduels.queue;

import com.ejyqyl.atomduels.arena.Arena;
import com.ejyqyl.atomduels.arena.ArenaManager;
import com.ejyqyl.atomduels.data.PlayerData;
import com.ejyqyl.atomduels.data.StatsManager;
import com.ejyqyl.atomduels.duel.DuelManager;
import com.ejyqyl.atomduels.rules.RuleSet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Matchmaking queue with expanding Elo search window (+-50 every 5 seconds up to +-500).
 * Authored by ejyqyl.
 */
public class QueueManager {

    public static class QueueEntry {
        private final UUID playerUuid;
        private final String kitName;
        private final boolean ranked;
        private final int initialElo;
        private final long joinedTime;

        public QueueEntry(UUID playerUuid, String kitName, boolean ranked, int initialElo) {
            this.playerUuid = playerUuid;
            this.kitName = kitName;
            this.ranked = ranked;
            this.initialElo = initialElo;
            this.joinedTime = System.currentTimeMillis();
        }

        public UUID getPlayerUuid() {
            return playerUuid;
        }

        public String getKitName() {
            return kitName;
        }

        public boolean isRanked() {
            return ranked;
        }

        public int getInitialElo() {
            return initialElo;
        }

        public int getSearchRadius() {
            long secondsInQueue = (System.currentTimeMillis() - joinedTime) / 1000L;
            int radius = 50 + (int) ((secondsInQueue / 5) * 50);
            return Math.min(500, radius);
        }
    }

    private final Plugin plugin;
    private final DuelManager duelManager;
    private final ArenaManager arenaManager;
    private final StatsManager statsManager;

    private final Map<UUID, QueueEntry> queue = new ConcurrentHashMap<>();
    private BukkitTask matcherTask;

    public QueueManager(Plugin plugin, DuelManager duelManager, ArenaManager arenaManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
        this.arenaManager = arenaManager;
        this.statsManager = statsManager;
        startMatcher();
    }

    public boolean isInQueue(UUID uuid) {
        return queue.containsKey(uuid);
    }

    public void addToQueue(Player player, String kitName, boolean ranked) {
        if (duelManager.isInDuel(player.getUniqueId()) || isInQueue(player.getUniqueId())) {
            return;
        }
        PlayerData data = statsManager.getPlayerData(player);
        queue.put(player.getUniqueId(), new QueueEntry(player.getUniqueId(), kitName, ranked, data.getElo()));
    }

    public void removeFromQueue(UUID uuid) {
        queue.remove(uuid);
    }

    private void startMatcher() {
        matcherTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (queue.size() < 2) {
                return;
            }

            List<QueueEntry> entries = new ArrayList<>(queue.values());
            List<UUID> matched = new ArrayList<>();

            for (int i = 0; i < entries.size(); i++) {
                QueueEntry a = entries.get(i);
                if (matched.contains(a.getPlayerUuid())) continue;

                Player playerA = Bukkit.getPlayer(a.getPlayerUuid());
                if (playerA == null || !playerA.isOnline() || duelManager.isInDuel(a.getPlayerUuid())) {
                    queue.remove(a.getPlayerUuid());
                    continue;
                }

                for (int j = i + 1; j < entries.size(); j++) {
                    QueueEntry b = entries.get(j);
                    if (matched.contains(b.getPlayerUuid())) continue;

                    Player playerB = Bukkit.getPlayer(b.getPlayerUuid());
                    if (playerB == null || !playerB.isOnline() || duelManager.isInDuel(b.getPlayerUuid())) {
                        queue.remove(b.getPlayerUuid());
                        continue;
                    }

                    // Check compatibility
                    if (a.isRanked() == b.isRanked() && a.getKitName().equalsIgnoreCase(b.getKitName())) {
                        boolean eloCompatible = true;
                        if (a.isRanked()) {
                            int eloDiff = Math.abs(a.getInitialElo() - b.getInitialElo());
                            int maxAllowed = Math.max(a.getSearchRadius(), b.getSearchRadius());
                            eloCompatible = eloDiff <= maxAllowed;
                        }

                        if (eloCompatible) {
                            Arena arena = arenaManager.getRandomAvailableArena();
                            if (arena != null) {
                                matched.add(a.getPlayerUuid());
                                matched.add(b.getPlayerUuid());
                                queue.remove(a.getPlayerUuid());
                                queue.remove(b.getPlayerUuid());

                                RuleSet ruleSet = new RuleSet();
                                ruleSet.setKitName(a.getKitName());
                                ruleSet.setMode(a.isRanked() ? RuleSet.DuelMode.RANKED : RuleSet.DuelMode.CASUAL);

                                duelManager.startMatch(playerA, playerB, ruleSet, arena);
                                break;
                            }
                        }
                    }
                }
            }
        }, 20L, 20L); // checks every second
    }

    public void shutdown() {
        if (matcherTask != null) {
            matcherTask.cancel();
            matcherTask = null;
        }
        queue.clear();
    }
}
