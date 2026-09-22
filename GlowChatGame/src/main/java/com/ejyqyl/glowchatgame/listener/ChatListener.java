package com.ejyqyl.glowchatgame.listener;

import com.ejyqyl.glowchatgame.GlowChatGame;
import com.ejyqyl.glowchatgame.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * High-speed asynchronous listener for chat messages.
 * Filters for numeric answers during active chat games without imposing chat latency.
 *
 * @author ejyqyl, Glowdevv
 */
public final class ChatListener implements Listener {

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^-?\\d+$");

    private final GlowChatGame plugin;
    private final GameManager gameManager;

    public ChatListener(@NotNull GlowChatGame plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (!gameManager.isGameRunning()) {
            return;
        }

        String rawMessage = event.getMessage().trim();

        // Check if the chat message looks like a pure numeric response
        if (!NUMERIC_PATTERN.matcher(rawMessage).matches()) {
            return;
        }

        Player player = event.getPlayer();
        gameManager.processChatAnswer(player, rawMessage);
    }
}
