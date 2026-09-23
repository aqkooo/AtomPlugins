package glowseller.utils;

import glowseller.Main;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Level;

public final class SyntaxParser {
    public enum ActionType {
        OPEN_GUI,
        BUY,
        SELL_ALL,
        CLOSE,
        CONSOLE_COMMAND,
        PLAYER_COMMAND,
        MESSAGE,
        SOUND,
        UNKNOWN
    }

    public static class Action {
        private final ActionType type;
        private final String argument;

        public Action(ActionType type, String argument) {
            this.type = type;
            this.argument = argument != null ? argument.trim() : "";
        }

        public ActionType getType() {
            return type;
        }

        public String getArgument() {
            return argument;
        }
    }

    private SyntaxParser() {}

    public static Action parse(String rawAction) {
        if (rawAction == null || rawAction.trim().isEmpty()) {
            return new Action(ActionType.UNKNOWN, "");
        }

        String trimmed = rawAction.trim();
        int closeBracket = trimmed.indexOf(']');
        if (!trimmed.startsWith("[") || closeBracket <= 1) {
            return new Action(ActionType.UNKNOWN, trimmed);
        }

        String tag = trimmed.substring(1, closeBracket).toLowerCase();
        String payload = trimmed.substring(closeBracket + 1).trim();

        return switch (tag) {
            case "opengui", "open", "gui" -> new Action(ActionType.OPEN_GUI, payload);
            case "buy" -> new Action(ActionType.BUY, payload);
            case "sell_all", "sell", "sellall" -> new Action(ActionType.SELL_ALL, payload);
            case "close" -> new Action(ActionType.CLOSE, payload);
            case "command", "console" -> new Action(ActionType.CONSOLE_COMMAND, payload);
            case "player" -> new Action(ActionType.PLAYER_COMMAND, payload);
            case "message", "msg" -> new Action(ActionType.MESSAGE, payload);
            case "sound" -> new Action(ActionType.SOUND, payload);
            default -> new Action(ActionType.UNKNOWN, payload);
        };
    }

    public static void execute(Main plugin, Player player, String rawAction) {
        if (player == null || rawAction == null) return;
        Action action = parse(rawAction);
        String arg = action.getArgument().replace("%player%", player.getName());

        switch (action.getType()) {
            case OPEN_GUI -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getMenuManager().openMenuByName(player, arg);
                });
            }
            case BUY -> {
                plugin.getShopManager().purchase(player, arg);
            }
            case SELL_ALL -> {
                plugin.getSellManager().sellInventory(player);
            }
            case CLOSE -> {
                Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
            }
            case CONSOLE_COMMAND -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), arg);
                });
            }
            case PLAYER_COMMAND -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.performCommand(arg.startsWith("/") ? arg.substring(1) : arg);
                });
            }
            case MESSAGE -> {
                player.sendMessage(Colorizer.colorize(arg));
            }
            case SOUND -> {
                try {
                    Sound sound = Sound.valueOf(arg.toUpperCase());
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING, "Invalid sound in action: " + arg);
                }
            }
            case UNKNOWN -> {
                // If no tag is recognized, do nothing or log fine
            }
        }
    }

    public static void executeAll(Main plugin, Player player, List<String> rawActions) {
        if (rawActions == null || rawActions.isEmpty()) return;
        for (String action : rawActions) {
            execute(plugin, player, action);
        }
    }
}
