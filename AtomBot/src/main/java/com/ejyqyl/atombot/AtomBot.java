package com.ejyqyl.atombot;

import com.ejyqyl.atombot.command.BotCommand;
import com.ejyqyl.atombot.listener.BotCombatListener;
import com.ejyqyl.atombot.listener.PlayerQuitListener;
import com.ejyqyl.atombot.manager.BotManager;
import com.ejyqyl.atombot.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * AtomBot — Standalone Training NPC Bot plugin for Paper 1.21+.
 * Built for comprehensive PvP practice, shield defense, combo training, and custom armor.
 *
 * @author ejyqyl (https://github.com/aqkooo)
 */
public class AtomBot extends JavaPlugin {

    private static AtomBot instance;
    private BotManager botManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.botManager = new BotManager(this);

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(new BotCombatListener(botManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(botManager), this);

        // Register command
        BotCommand cmd = new BotCommand(this);
        PluginCommand botCmd = getCommand("bot");
        if (botCmd != null) {
            botCmd.setExecutor(cmd);
            botCmd.setTabCompleter(cmd);
        }

        printBanner();
        getLogger().info("AtomBot v" + getDescription().getVersion() + " by ejyqyl successfully initialized!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down AtomBot safely...");
        if (botManager != null) {
            botManager.cleanupAll();
        }
        getLogger().info("AtomBot stopped gracefully.");
    }

    public void sendMessage(Player player, String configPath) {
        String prefix = getConfig().getString("prefix", "<gradient:#00B5FD:#7670E5>ᴀᴛᴏᴍʙᴏᴛ ≫</gradient>");
        String raw = getConfig().getString(configPath, "");
        if (raw.isEmpty()) return;

        String formatted = raw.replace("{prefix}", prefix);
        player.sendMessage(ColorUtil.parse(formatted));
    }

    private void printBanner() {
        String[] banner = new String[] {
            "  <gradient:#00B5FD:#7670E5>█████╗ ████████╗ ██████╗ ███╗   ███╗██████╗  ██████╗ ████████╗</gradient>",
            "  <gradient:#00B5FD:#7670E5>██╔══██╗╚══██╔══╝██╔═══██╗████╗ ████║██╔══██╗██╔═══██╗╚══██╔══╝</gradient>",
            "  <gradient:#00B5FD:#7670E5>███████║   ██║   ██║   ██║██╔████╔██║██████╔╝██║   ██║   ██║   </gradient>",
            "  <gradient:#00B5FD:#7670E5>██╔══██║   ██║   ██║   ██║██║╚██╔╝██║██╔══██╗██║   ██║   ██║   </gradient>",
            "  <gradient:#00B5FD:#7670E5>██║  ██║   ██║   ╚██████╔╝██║ ╚═╝ ██║██████╔╝╚██████╔╝   ██║   </gradient>",
            "  <gradient:#00B5FD:#7670E5>╚═╝  ╚═╝   ╚═╝    ╚═════╝ ╚═╝     ╚═╝╚═════╝  ╚═════╝    ╚═╝   </gradient>",
            "  <gradient:#00FF88:#00B5FD>        AtomBot v1.0.0 | Training NPC Bot | Author: ejyqyl            </gradient>",
            "  <gradient:#00FF88:#00B5FD>               GitHub: https://github.com/aqkooo                      </gradient>"
        };

        for (String line : banner) {
            Bukkit.getConsoleSender().sendMessage(ColorUtil.parse(line));
        }
    }

    public static AtomBot getInstance() {
        return instance;
    }

    public BotManager getBotManager() {
        return botManager;
    }
}
