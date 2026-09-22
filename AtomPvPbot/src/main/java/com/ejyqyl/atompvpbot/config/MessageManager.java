package com.ejyqyl.atompvpbot.config;

import com.ejyqyl.atompvpbot.util.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Localization and message delivery engine for AtomPvPbot.
 *
 * @author ejyqyl
 */
public class MessageManager {

    private final Plugin plugin;
    private FileConfiguration messagesConfig;
    private final File messagesFile;

    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        loadMessages();
    }

    public void loadMessages() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, Map.of());
    }

    public void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        if (sender == null || messagesConfig == null) return;

        String prefix = messagesConfig.getString("prefix", "<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient>");

        if (messagesConfig.isList(key)) {
            List<String> lines = messagesConfig.getStringList(key);
            for (String line : lines) {
                sender.sendMessage(ColorUtil.parse(format(line, prefix, placeholders)));
            }
        } else {
            String raw = messagesConfig.getString(key);
            if (raw != null && !raw.isEmpty()) {
                sender.sendMessage(ColorUtil.parse(format(raw, prefix, placeholders)));
            }
        }
    }

    public void sendRawMessage(CommandSender sender, String text) {
        if (sender == null || text == null) return;
        String prefix = messagesConfig != null ? messagesConfig.getString("prefix", "<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴘᴠᴘʙᴏᴛ ≫</gradient>") : "";
        sender.sendMessage(ColorUtil.parse(text.replace("{prefix}", prefix)));
    }

    public String getRaw(String key) {
        return messagesConfig != null ? messagesConfig.getString(key, "") : "";
    }

    private String format(String message, String prefix, Map<String, String> placeholders) {
        String result = message.replace("{prefix}", prefix);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
