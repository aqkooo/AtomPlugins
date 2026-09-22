package ru.atomicsqd.glowtrade;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.atomicsqd.glowtrade.command.TradeCommand;
import ru.atomicsqd.glowtrade.config.ConfigManager;
import ru.atomicsqd.glowtrade.hook.CombatHook;
import ru.atomicsqd.glowtrade.listener.TradeListener;
import ru.atomicsqd.glowtrade.manager.TradeManager;
import ru.atomicsqd.glowtrade.util.ColorUtil;
import ru.atomicsqd.glowtrade.util.ItemUtil;

/**
 * GlowTrade — Высокопроизводительный, надежный и защищенный от дюпов плагин безопасного обмена ресурсами.
 *
 * @author ejyqyl / atomicsqd
 * @version 1.0.0
 */
public class GlowTrade extends JavaPlugin {

    private static GlowTrade instance;

    private ConfigManager configManager;
    private CombatHook combatHook;
    private TradeManager tradeManager;

    @Override
    public void onEnable() {
        instance = this;

        // Инициализация утилит
        ItemUtil.init(this);

        // Инициализация менеджера конфигураций
        this.configManager = new ConfigManager(this);

        // Инициализация хуков боя (CombatLogX / PvPManager)
        this.combatHook = new CombatHook();

        // Инициализация менеджера сессий обмена
        this.tradeManager = new TradeManager(this, configManager, combatHook);

        // Регистрация слушателей событий
        TradeListener tradeListener = new TradeListener(this, configManager, tradeManager, combatHook);
        Bukkit.getPluginManager().registerEvents(tradeListener, this);

        // Регистрация команд
        TradeCommand tradeCommand = new TradeCommand(configManager, tradeManager);

        PluginCommand cmdTrade = getCommand("trade");
        if (cmdTrade != null) {
            cmdTrade.setExecutor(tradeCommand);
            cmdTrade.setTabCompleter(tradeCommand);
        }

        PluginCommand cmdGlowTrade = getCommand("glowtrade");
        if (cmdGlowTrade != null) {
            cmdGlowTrade.setExecutor(tradeCommand);
            cmdGlowTrade.setTabCompleter(tradeCommand);
        }

        // Лог запуска в фирменном стиле GlowTrade
        sendConsoleBanner();
    }

    @Override
    public void onDisable() {
        // Гарантированная отмена всех активных трейдов с возвратом ресурсов до выгрузки плагина
        if (tradeManager != null) {
            tradeManager.cancelAllTrades("Сервер перезагружается / плагин GlowTrade выключается");
        }

        // Отменяем все фоновые таймеры плагина
        Bukkit.getScheduler().cancelTasks(this);

        getLogger().info(ColorUtil.colorize("&6[GlowTrade] &cПлагин успешно выключен. Все активные обмены безопасно завершены."));
    }

    private void sendConsoleBanner() {
        String line = "==========================================================";
        String banner = """
                
                %s
                   _____ _               _______             _      \s
                  / ____| |             |__   __|           | |     \s
                 | |  __| | _____      __  | |_ __ __ _   __| | ___ \s
                 | | |_ | |/ _ \\ \\ /\\ / /  | | '__/ _` | / _` |/ _ \\\s
                 | |__| | | (_) \\ V  V /   | | | | (_| || (_| |  __/\s
                  \\_____|_|\\___/ \\_/\\_/    |_|_|  \\__,_| \\__,_|\\___|\s
                  Safe Player-to-Player Trade Plugin | ReallyWorld Style
                  Author: ejyqyl / atomicsqd | Version: 1.0.0
                %s
                """.formatted(line, line);

        getLogger().info(ColorUtil.colorize("&e" + banner));
        getLogger().info(ColorUtil.colorize("&a[GlowTrade] &6Плагин успешно запущен и готов к работе!"));
    }

    public static GlowTrade getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CombatHook getCombatHook() {
        return combatHook;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }
}
