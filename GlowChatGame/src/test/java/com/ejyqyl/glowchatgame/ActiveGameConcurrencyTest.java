package com.ejyqyl.glowchatgame;

import com.ejyqyl.glowchatgame.game.ActiveGame;
import com.ejyqyl.glowchatgame.game.Difficulty;
import com.ejyqyl.glowchatgame.game.MathProblem;
import com.ejyqyl.glowchatgame.reward.RewardGroup;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ActiveGameConcurrencyTest {

    @Test
    void testConcurrentAnswerRaceCondition() throws InterruptedException {
        MathProblem problem = new MathProblem("25 + 25", 50, Difficulty.EASY);
        RewardGroup reward = new RewardGroup(100, "$500", List.of("eco give %player% 500"));
        ActiveGame game = new ActiveGame(problem, reward, 30);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger winCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // wait for simultaneous start
                    Player player = Mockito.mock(Player.class);
                    when(player.getName()).thenReturn("Player" + index);

                    if (game.trySolve(player, "50")) {
                        winCount.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        // Exactly ONE thread must have won!
        assertEquals(1, winCount.get(), "AtomicBoolean must ensure exactly one winner even under race conditions");
        assertTrue(game.isSolved());
        assertNotNull(game.getWinner());
    }
}
