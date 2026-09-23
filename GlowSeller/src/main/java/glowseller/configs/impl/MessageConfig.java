package glowseller.configs.impl;

import glowseller.Main;
import glowseller.configs.CustomConfig;
import glowseller.utils.Colorizer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class MessageConfig extends CustomConfig {
    private final Map<String, String> messages = new HashMap<>();
    private String prefix = "";

    public MessageConfig(Main plugin) {
        super(plugin, "messages.yml");
        reload();
    }

    @Override
    public void parse() {
        messages.clear();
        prefix = config.getString("prefix", "&8[&b&lGlowSeller&8] &f");

        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                messages.put(key, config.getString(key));
            }
        }
    }

    public String getRaw(String key) {
        return messages.getOrDefault(key, key);
    }

    public String get(String key, Object... replacements) {
        String msg = messages.getOrDefault(key, "&cMissing message: " + key);
        msg = msg.replace("%prefix%", prefix);

        if (replacements != null && replacements.length > 1) {
            for (int i = 0; i < replacements.length; i += 2) {
                String target = String.valueOf(replacements[i]);
                String val = (i + 1 < replacements.length && replacements[i + 1] != null)
                        ? String.valueOf(replacements[i + 1])
                        : "";
                msg = msg.replace(target, val);
            }
        }

        return Colorizer.colorize(msg);
    }

    public void send(CommandSender sender, String key, Object... replacements) {
        if (sender == null) return;
        String formatted = get(key, replacements);
        if (!formatted.isEmpty()) {
            sender.sendMessage(formatted);
        }
    }

    public void sendActionBar(Player player, String key, Object... replacements) {
        if (player == null) return;
        String formatted = get(key, replacements);
        if (!formatted.isEmpty()) {
            try {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(formatted));
            } catch (Exception e) {
                player.sendMessage(formatted);
            }
        }
    }

    public void sendTitle(Player player, String titleKey, String subtitleKey, Object... replacements) {
        if (player == null) return;
        String title = titleKey != null ? get(titleKey, replacements) : "";
        String subtitle = subtitleKey != null ? get(subtitleKey, replacements) : "";
        player.sendTitle(title, subtitle, 10, 50, 20);
    }
}
