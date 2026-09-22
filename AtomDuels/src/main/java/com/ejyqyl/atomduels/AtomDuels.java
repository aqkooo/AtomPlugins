package com.ejyqyl.atomduels;

import com.ejyqyl.atomduels.arena.ArenaManager;
import com.ejyqyl.atomduels.bot.BotManager;
import com.ejyqyl.atomduels.command.DuelsCommand;
import com.ejyqyl.atomduels.config.ConfigManager;
import com.ejyqyl.atomduels.config.MessageManager;
import com.ejyqyl.atomduels.data.BetManager;
import com.ejyqyl.atomduels.data.CrashRecoveryManager;
import com.ejyqyl.atomduels.data.DatabaseManager;
import com.ejyqyl.atomduels.data.StatsManager;
import com.ejyqyl.atomduels.duel.DuelManager;
import com.ejyqyl.atomduels.gui.MenuManager;
import com.ejyqyl.atomduels.hook.PlaceholderHook;
import com.ejyqyl.atomduels.hook.VaultHook;
import com.ejyqyl.atomduels.kit.KitManager;
import com.ejyqyl.atomduels.listener.AntiAbuseListener;
import com.ejyqyl.atomduels.listener.BlockTrackerListener;
import com.ejyqyl.atomduels.listener.CombatListener;
import com.ejyqyl.atomduels.listener.PlayerConnectionListener;
import com.ejyqyl.atomduels.queue.QueueManager;
import com.ejyqyl.atomduels.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * AtomDuels — Enterprise PvP Duels, Escrow Betting & Zero-Sum Matchmaking.
 * Authored by ejyqyl (https://github.com/aqkooo) for Paper 1.21+.
 */
public class AtomDuels extends JavaPlugin {

    private static AtomDuels instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private StatsManager statsManager;
    private VaultHook vaultHook;
    private BetManager betManager;
    private ArenaManager arenaManager;
    private KitManager kitManager;
    private BotManager botManager;
    private DuelManager duelManager;
    private QueueManager queueManager;
    private PlaceholderHook placeholderHook;
    private CrashRecoveryManager crashRecoveryManager;

    @Override
    public void onEnable() {
        instance = this;

        // Print grand atomicsqd ASCII banner on startup
        printBanner();

        // 1. Configs & Localization
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);

        // 2. Database & Statistics
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.init();
        this.statsManager = new StatsManager(databaseManager);

        // 3. Economy & Escrow
        this.vaultHook = new VaultHook(this);
        this.betManager = new BetManager(this, vaultHook, databaseManager, statsManager);

        // 4. Arenas & Kits
        this.arenaManager = new ArenaManager(this);
        this.arenaManager.loadArenas();
        this.kitManager = new KitManager(this);
        this.kitManager.loadKits();

        // 5. Bot & Duel Managers
        this.botManager = new BotManager(this);
        this.duelManager = new DuelManager(this, configManager, messageManager, arenaManager, kitManager, statsManager, betManager, botManager, databaseManager);
        this.queueManager = new QueueManager(this, duelManager, arenaManager, statsManager);

        // 6. Crash Recovery
        this.crashRecoveryManager = new CrashRecoveryManager(this, databaseManager, betManager);
        this.crashRecoveryManager.recoverInterruptedDuels();

        // 7. Hooks & Integrations
        this.placeholderHook = new PlaceholderHook(this);

        // 8. Register Event Listeners
        Bukkit.getPluginManager().registerEvents(new CombatListener(duelManager, botManager), this);
        Bukkit.getPluginManager().registerEvents(new BlockTrackerListener(duelManager, arenaManager), this);
        Bukkit.getPluginManager().registerEvents(new AntiAbuseListener(duelManager, configManager, messageManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(statsManager, duelManager, queueManager, messageManager, botManager), this);
        Bukkit.getPluginManager().registerEvents(new MenuManager(), this);

        // 9. Register Commands
        DuelsCommand cmdHandler = new DuelsCommand(this);
        PluginCommand cmd = getCommand("duels");
        if (cmd != null) {
            cmd.setExecutor(cmdHandler);
            cmd.setTabCompleter(cmdHandler);
        }

        getLogger().info("AtomDuels v" + getDescription().getVersion() + " by ejyqyl successfully initialized!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down AtomDuels safely...");

        if (queueManager != null) {
            queueManager.shutdown();
        }

        if (duelManager != null) {
            duelManager.cleanup();
        }

        if (statsManager != null) {
            statsManager.saveAll();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("AtomDuels stopped gracefully.");
    }

    public void reloadAll() {
        configManager.loadConfig();
        messageManager.loadMessages();
        arenaManager.loadArenas();
        kitManager.loadKits();
    }

    private void printBanner() {
        String[] banner = new String[] {
            "  <gradient:#00B5FD:#7670E5>█████╗ ████████╗ ██████╗ ███╗   ███╗██╗ ██████╗███████╗ ██████╗ ██████╗ </gradient>",
            "  <gradient:#00B5FD:#7670E5>██╔══██╗╚══██╔══╝██╔═══██╗████╗ ████║██║██╔════╝██╔════╝██╔═══██╗██╔══██╗</gradient>",
            "  <gradient:#00B5FD:#7670E5>███████║   ██║   ██║   ██║██╔████╔██║██║██║     ███████╗██║   ██║██║  ██║</gradient>",
            "  <gradient:#00B5FD:#7670E5>██╔══██║   ██║   ██║   ██║██║╚██╔╝██║██║██║     ╚════██║██║▄▄ ██║██║  ██║</gradient>",
            "  <gradient:#00B5FD:#7670E5>██║  ██║   ██║   ╚██████╔╝██║ ╚═╝ ██║██║╚██████╗███████║╚██████╔╝██████╔╝</gradient>",
            "  <gradient:#00B5FD:#7670E5>╚═╝  ╚═╝   ╚═╝    ╚═════╝ ╚═╝     ╚═╝╚═╝ ╚═════╝╚══════╝ ╚══▀▀═╝ ╚═════╝ </gradient>",
            "  <gradient:#00FF88:#00B5FD>      AtomDuels v1.0.0 | Paper 1.21+ | Author: ejyqyl (aqkooo)      </gradient>",
            "  <gradient:#00FF88:#00B5FD>            GitHub: https://github.com/aqkooo                     </gradient>"
        };

        for (String line : banner) {
            Bukkit.getConsoleSender().sendMessage(ColorUtil.parse(line));
        }
    }

    public static AtomDuels getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public BetManager getBetManager() {
        return betManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public BotManager getBotManager() {
        return botManager;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }
}
