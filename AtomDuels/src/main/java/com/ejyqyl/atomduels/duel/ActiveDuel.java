package com.ejyqyl.atomduels.duel;

import com.ejyqyl.atomduels.arena.Arena;
import com.ejyqyl.atomduels.arena.BlockTracker;
import com.ejyqyl.atomduels.bot.DuelBot;
import com.ejyqyl.atomduels.kit.Kit;
import com.ejyqyl.atomduels.rules.RuleSet;
import com.ejyqyl.atomduels.util.InventorySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Encapsulates the state, participants, rounds, and arena of an ongoing duel match.
 * Authored by ejyqyl.
 */
public class ActiveDuel {

    private final UUID duelId = UUID.randomUUID();
    private final UUID player1;
    private final UUID player2;
    private DuelBot bot;
    private final Arena arena;
    private final Kit kit;
    private final RuleSet ruleSet;
    private DuelState state = DuelState.COUNTDOWN;

    private int currentRound = 1;
    private int p1RoundWins = 0;
    private int p2RoundWins = 0;
    private final List<DuelRound> rounds = new ArrayList<>();

    private final BlockTracker blockTracker = new BlockTracker();
    private InventorySnapshot snapshotP1;
    private InventorySnapshot snapshotP2;
    private final Set<UUID> spectators = new HashSet<>();
    private final long startTime = System.currentTimeMillis();

    public ActiveDuel(UUID player1, UUID player2, Arena arena, Kit kit, RuleSet ruleSet) {
        this.player1 = player1;
        this.player2 = player2;
        this.arena = arena;
        this.kit = kit;
        this.ruleSet = ruleSet;
        this.rounds.add(new DuelRound(1));
    }

    public UUID getDuelId() {
        return duelId;
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public boolean isBotMatch() {
        return bot != null || player2 == null;
    }

    public DuelBot getBot() {
        return bot;
    }

    public void setBot(DuelBot bot) {
        this.bot = bot;
    }

    public Arena getArena() {
        return arena;
    }

    public Kit getKit() {
        return kit;
    }

    public RuleSet getRuleSet() {
        return ruleSet;
    }

    public DuelState getState() {
        return state;
    }

    public void setState(DuelState state) {
        this.state = state;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getP1RoundWins() {
        return p1RoundWins;
    }

    public void incrementP1RoundWins() {
        this.p1RoundWins++;
    }

    public int getP2RoundWins() {
        return p2RoundWins;
    }

    public void incrementP2RoundWins() {
        this.p2RoundWins++;
    }

    public List<DuelRound> getRounds() {
        return Collections.unmodifiableList(rounds);
    }

    public DuelRound getCurrentDuelRound() {
        if (rounds.isEmpty()) {
            DuelRound r = new DuelRound(currentRound);
            rounds.add(r);
            return r;
        }
        return rounds.get(rounds.size() - 1);
    }

    public void startNextRound() {
        currentRound++;
        rounds.add(new DuelRound(currentRound));
    }

    public BlockTracker getBlockTracker() {
        return blockTracker;
    }

    public InventorySnapshot getSnapshotP1() {
        return snapshotP1;
    }

    public void setSnapshotP1(InventorySnapshot snapshotP1) {
        this.snapshotP1 = snapshotP1;
    }

    public InventorySnapshot getSnapshotP2() {
        return snapshotP2;
    }

    public void setSnapshotP2(InventorySnapshot snapshotP2) {
        this.snapshotP2 = snapshotP2;
    }

    public Set<UUID> getSpectators() {
        return spectators;
    }

    public void addSpectator(UUID uuid) {
        spectators.add(uuid);
    }

    public void removeSpectator(UUID uuid) {
        spectators.remove(uuid);
    }

    public boolean isParticipant(UUID uuid) {
        if (uuid == null) return false;
        return uuid.equals(player1) || uuid.equals(player2);
    }

    public UUID getOpponent(UUID uuid) {
        if (uuid == null) return null;
        if (uuid.equals(player1)) return player2;
        if (uuid.equals(player2)) return player1;
        return null;
    }

    public long getStartTime() {
        return startTime;
    }
}
