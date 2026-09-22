package ru.glowdev.glowsnake.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import ru.glowdev.glowsnake.GlowSnakePlugin;
import ru.glowdev.glowsnake.config.ConfigManager;
import ru.glowdev.glowsnake.game.Direction;
import ru.glowdev.glowsnake.game.SnakeGameSession;

public class PlayerMovementListener implements Listener {

    private final GlowSnakePlugin plugin;

    public PlayerMovementListener(GlowSnakePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        SnakeGameSession session = plugin.getGameManager().getSession(player);
        if (session == null || session.isGameOver() || session.isClosed()) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        ConfigManager cfg = plugin.getConfigManager();

        if (cfg.isEnableWasd()) {
            double dx = to.getX() - from.getX();
            double dz = to.getZ() - from.getZ();
            double dy = to.getY() - from.getY();

            // Jump -> UP, Sneak -> DOWN
            if (dy > 0.08) {
                session.changeDirection(Direction.UP);
            } else if (player.isSneaking()) {
                session.changeDirection(Direction.DOWN);
            }

            double distSq = dx * dx + dz * dz;
            double sensitivity = cfg.getSensitivity();
            if (distSq > sensitivity * sensitivity) {
                double yawRad = Math.toRadians(from.getYaw());

                // Minecraft coordinate system:
                // Forward vector: (-sin(yaw), cos(yaw))
                // Right vector:   (cos(yaw), sin(yaw))
                double forward = -dx * Math.sin(yawRad) + dz * Math.cos(yawRad);
                double strafe = dx * Math.cos(yawRad) + dz * Math.sin(yawRad);

                Direction requested = null;
                if (Math.abs(forward) >= Math.abs(strafe)) {
                    if (forward > sensitivity) {
                        requested = Direction.UP; // W
                    } else if (forward < -sensitivity) {
                        requested = Direction.DOWN; // S
                    }
                } else {
                    if (strafe > sensitivity) {
                        requested = Direction.RIGHT; // D
                    } else if (strafe < -sensitivity) {
                        requested = Direction.LEFT; // A
                    }
                }

                if (requested != null) {
                    session.changeDirection(requested);
                }
            }
        }

        // Keep player fixed near origin location
        Location origin = session.getOriginLocation();
        if (to.distanceSquared(origin) > 0.04) {
            event.setTo(new Location(
                    origin.getWorld(),
                    origin.getX(),
                    origin.getY(),
                    origin.getZ(),
                    to.getYaw(),
                    to.getPitch()
            ));
        }
    }
}
