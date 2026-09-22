package com.ejyqyl.glowcmd;

import com.ejyqyl.glowcmd.command.GlowCMDCommand;
import com.ejyqyl.glowcmd.command.SetFirstSpawnCommand;
import com.ejyqyl.glowcmd.command.SetSpawnCommand;
import com.ejyqyl.glowcmd.command.SpawnCommand;
import com.ejyqyl.glowcmd.command.admin.*;
import com.ejyqyl.glowcmd.command.home.*;
import com.ejyqyl.glowcmd.command.msg.*;
import com.ejyqyl.glowcmd.command.player.*;
import com.ejyqyl.glowcmd.command.teleport.*;
import com.ejyqyl.glowcmd.command.warp.*;
import com.ejyqyl.glowcmd.command.world.*;
import com.ejyqyl.glowcmd.config.ConfigManager;
import com.ejyqyl.glowcmd.config.MessageManager;
import com.ejyqyl.glowcmd.hook.PlaceholderHook;
import com.ejyqyl.glowcmd.listener.PlayerJoinListener;
import com.ejyqyl.glowcmd.listener.PlayerRespawnListener;
import com.ejyqyl.glowcmd.listener.TeleportListener;
import com.ejyqyl.glowcmd.manager.*;
import com.ejyqyl.glowcmd.spawn.SpawnManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * GlowCMD - Ultra-optimized EssentialsX and CMI fork for modern Minecraft servers.
 *
 * @author ejyqyl
 */
public final class GlowCMD extends JavaPlugin {

    private static GlowCMD instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private SpawnManager spawnManager;

    private TeleportManager teleportManager;
    private HomeManager homeManager;
    private WarpManager warpManager;
    private VanishManager vanishManager;
    private PrivateMessageManager privateMessageManager;
    private PlaceholderHook placeholderHook;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Initialize core managers
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.spawnManager = new SpawnManager(this);

        // 2. Initialize feature managers
        this.teleportManager = new TeleportManager(this);
        this.homeManager = new HomeManager(this);
        this.warpManager = new WarpManager(this);
        this.vanishManager = new VanishManager(this);
        this.privateMessageManager = new PrivateMessageManager();

        // 3. Register all commands
        registerCommands();

        // 4. Register event listeners
        registerListeners();

        // 5. Hook into PlaceholderAPI
        this.placeholderHook = new PlaceholderHook(this);
        this.placeholderHook.register();

        getLogger().info("GlowCMD v" + getDescription().getVersion() + " by ejyqyl enabled with complete EssentialsX & CMI command suites.");
    }

    @Override
    public void onDisable() {
        if (placeholderHook != null) {
            placeholderHook.unregister();
        }

        // Flush all persistent storage synchronously on shutdown
        if (spawnManager != null) spawnManager.saveSync();
        if (homeManager != null) homeManager.saveSync();
        if (warpManager != null) warpManager.saveSync();

        getServer().getScheduler().cancelTasks(this);
        getLogger().info("GlowCMD disabled successfully.");
        instance = null;
    }

    public synchronized void reloadAll() {
        if (spawnManager != null) spawnManager.saveSync();
        if (homeManager != null) homeManager.saveSync();
        if (warpManager != null) warpManager.saveSync();

        if (configManager != null) configManager.reload();
        if (messageManager != null) messageManager.reload();
        if (spawnManager != null) spawnManager.load();
        if (homeManager != null) homeManager.load();
        if (warpManager != null) warpManager.load();

        getLogger().info("GlowCMD all configurations and data reloaded successfully.");
    }

    private void registerCommands() {
        // Administration
        registerCommand("glowcmd", new GlowCMDCommand(this));

        // Spawn
        registerCommand("spawn", new SpawnCommand(this));
        registerCommand("setspawn", new SetSpawnCommand(this));
        registerCommand("setfirstspawn", new SetFirstSpawnCommand(this));

        // Player Utilities
        registerCommand("heal", new HealCommand(this));
        registerCommand("feed", new FeedCommand(this));
        registerCommand("fly", new FlyCommand(this));
        registerCommand("god", new GodCommand(this));
        registerCommand("speed", new SpeedCommand(this));
        registerCommand("gamemode", new GamemodeCommand(this));
        registerCommand("clear", new ClearInventoryCommand(this));
        registerCommand("hat", new HatCommand(this));
        registerCommand("workbench", new WorkbenchCommand(this));
        registerCommand("enderchest", new EnderChestCommand(this));
        registerCommand("anvil", new AnvilCommand(this));
        registerCommand("top", new TopCommand(this));
        registerCommand("suicide", new SuicideCommand(this));
        registerCommand("extinguish", new ExtinguishCommand(this));

        // Teleportation Suite
        registerCommand("tp", new TeleportCommand(this));
        registerCommand("tphere", new TpHereCommand(this));
        registerCommand("tpa", new TpaCommand(this));
        registerCommand("tpahere", new TpaCommand(this));
        registerCommand("tpaccept", new TpAcceptCommand(this));
        registerCommand("tpdeny", new TpDenyCommand(this));
        registerCommand("tpcancel", new TpCancelCommand(this));
        registerCommand("back", new BackCommand(this));
        registerCommand("rtp", new RtpCommand(this));

        // Homes
        registerCommand("sethome", new SetHomeCommand(this));
        registerCommand("home", new HomeCommand(this));
        registerCommand("delhome", new DelHomeCommand(this));
        registerCommand("homes", new HomesCommand(this));

        // Warps
        registerCommand("setwarp", new SetWarpCommand(this));
        registerCommand("warp", new WarpCommand(this));
        registerCommand("delwarp", new DelWarpCommand(this));
        registerCommand("warps", new WarpsCommand(this));

        // Moderation & Admin
        registerCommand("vanish", new VanishCommand(this));
        registerCommand("invsee", new InvseeCommand(this));
        registerCommand("broadcast", new BroadcastCommand(this));
        registerCommand("sudo", new SudoCommand(this));
        registerCommand("ping", new PingCommand(this));
        registerCommand("seen", new SeenCommand(this));

        // World & Environment
        registerCommand("time", new TimeCommand(this));
        registerCommand("weather", new WeatherCommand(this));

        // Private Messaging
        registerCommand("msg", new MsgCommand(this));
        registerCommand("reply", new ReplyCommand(this));
    }

    private void registerCommand(@NotNull String name, @NotNull Object handler) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            if (handler instanceof org.bukkit.command.CommandExecutor executor) {
                cmd.setExecutor(executor);
            }
            if (handler instanceof org.bukkit.command.TabCompleter completer) {
                cmd.setTabCompleter(completer);
            }
        }
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new PlayerRespawnListener(this), this);
        pm.registerEvents(new TeleportListener(this), this);
    }

    public static GlowCMD getInstance() {
        return instance;
    }

    @NotNull
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @NotNull
    public MessageManager getMessageManager() {
        return messageManager;
    }

    @NotNull
    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    @NotNull
    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    @NotNull
    public HomeManager getHomeManager() {
        return homeManager;
    }

    @NotNull
    public WarpManager getWarpManager() {
        return warpManager;
    }

    @NotNull
    public VanishManager getVanishManager() {
        return vanishManager;
    }

    @NotNull
    public PrivateMessageManager getPrivateMessageManager() {
        return privateMessageManager;
    }

    public PlaceholderHook getPlaceholderHook() {
        return placeholderHook;
    }
}
