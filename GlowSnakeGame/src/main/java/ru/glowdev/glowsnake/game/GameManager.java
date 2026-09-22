package ru.glowdev.glowsnake.game;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.glowdev.glowsnake.GlowSnakePlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    private final GlowSnakePlugin plugin;
    private final Map<UUID, SnakeGameSession> activeSessions = new ConcurrentHashMap<>();

    public GameManager(GlowSnakePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasSession(Player player) {
        if (player == null) return false;
        return activeSessions.containsKey(player.getUniqueId());
    }

    public SnakeGameSession getSession(Player player) {
        if (player == null) return null;
        return activeSessions.get(player.getUniqueId());
    }

    public SnakeGameSession getSessionByInventory(Inventory inventory) {
        if (inventory == null) return null;
        for (SnakeGameSession session : activeSessions.values()) {
            if (inventory.equals(session.getInventory())) {
                return session;
            }
        }
        return null;
    }

    public void startGame(Player player) {
        if (player == null) return;

        // Cleanup existing session if present
        if (hasSession(player)) {
            stopGame(player);
        }

        SnakeGameSession session = new SnakeGameSession(plugin, player);
        activeSessions.put(player.getUniqueId(), session);
        session.start();
    }

    public void stopGame(Player player) {
        if (player == null) return;

        SnakeGameSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.cleanup();
        }
    }

    public void shutdown() {
        for (SnakeGameSession session : activeSessions.values()) {
            try {
                session.cleanup();
                if (session.getPlayer().isOnline()) {
                    session.getPlayer().closeInventory();
                }
            } catch (Exception ignored) {
            }
        }
        activeSessions.clear();
    }
}
