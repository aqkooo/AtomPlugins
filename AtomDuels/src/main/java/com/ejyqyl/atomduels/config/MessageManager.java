package com.ejyqyl.atomduels.config;

import com.ejyqyl.atomduels.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Message manager handling MiniMessage gradient formatting, placeholders, and chat output.
 * Authored by ejyqyl.
 */
public class MessageManager {

    private final Plugin plugin;
    private final File messagesFile;
    private YamlConfiguration config;

    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        loadMessages();
    }

    public void loadMessages() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getRaw(String path) {
        return config.getString(path, path);
    }

    public List<String> getRawList(String path) {
        List<String> list = config.getStringList(path);
        return list.isEmpty() ? Collections.singletonList(path) : list;
    }

    public String getPrefix() {
        return config.getString("prefix", "<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍᴅᴜᴇʟs ≫</gradient>");
    }

    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, Collections.emptyMap());
    }

    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        if (config.isList(path)) {
            List<String> lines = config.getStringList(path);
            for (String line : lines) {
                sender.sendMessage(format(line, placeholders));
            }
        } else {
            String raw = config.getString(path, path);
            sender.sendMessage(format(raw, placeholders));
        }
    }

    public void sendRawMessage(CommandSender sender, String message) {
        sender.sendMessage(ColorUtil.parse(message.replace("{prefix}", getPrefix())));
    }

    public Component format(String raw, Map<String, String> placeholders) {
        String msg = raw.replace("{prefix}", getPrefix());
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return ColorUtil.parse(msg);
    }
}
