package ru.atomicsqd.glowtrade.config;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import ru.atomicsqd.glowtrade.util.ColorUtil;

import java.io.File;
import java.util.*;

/**
 * Менеджер конфигураций плагина GlowTrade.
 * Обеспечивает загрузку, кеширование и безопасную перезагрузку config.yml и messages.yml.
 */
public class ConfigManager {

    private final Plugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File messagesFile;

    // Кешированные настройки
    private int requestTimeout;
    private int countdownSeconds;
    private boolean distanceEnabled;
    private double maxDistance;
    private boolean requireSameWorld;
    private boolean shiftClickRequest;

    private boolean preventCreative;
    private boolean preventSpectator;
    private boolean preventInCombat;
    private boolean cancelOnDamage;
    private boolean cancelOnDistanceExceeded;
    private boolean blockAnvilRenamedItems;

    private final List<Material> blacklistedMaterials = new ArrayList<>();
    private final List<Integer> blacklistedCmd = new ArrayList<>();
    private final List<String> blacklistedTags = new ArrayList<>();

    private boolean soundsEnabled;
    private final Map<String, String> soundNames = new HashMap<>();

    private String guiTitle;
    private Material separatorMaterial;
    private Material fillerMaterial;
    private Material notReadyMaterial;
    private Material readyMaterial;
    private Material timerWaitingMaterial;
    private Material timerActiveMaterial;

    private String prefix;
    private String rawPrefix;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        load();
    }

    /**
     * Загружает или перезагружает файлы конфигурации.
     */
    public void load() {
        // Загрузка config.yml
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Загрузка messages.yml
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        } else {
            // Авто-миграция существующих файлов конфигурации (AtomicSMP -> GlowTrade)
            try {
                String content = java.nio.file.Files.readString(messagesFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                if (content.contains("AtomicSMP") || content.contains("<#FF5555>")) {
                    content = content.replace("AtomicSMP", "GlowTrade");
                    java.nio.file.Files.writeString(messagesFile.toPath(), content, java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {}
        }
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Парсинг основных настроек
        this.requestTimeout = config.getInt("settings.request-timeout", 30);
        this.countdownSeconds = config.getInt("settings.countdown-seconds", 3);
        this.distanceEnabled = config.getBoolean("settings.distance.enabled", true);
        this.maxDistance = config.getDouble("settings.distance.max-distance", 15.0);
        this.requireSameWorld = config.getBoolean("settings.distance.require-same-world", true);
        this.shiftClickRequest = config.getBoolean("settings.shift-click-request", true);

        // Ограничения
        this.preventCreative = config.getBoolean("settings.restrictions.prevent-creative", true);
        this.preventSpectator = config.getBoolean("settings.restrictions.prevent-spectator", true);
        this.preventInCombat = config.getBoolean("settings.restrictions.prevent-in-combat", true);
        this.cancelOnDamage = config.getBoolean("settings.restrictions.cancel-on-damage", true);
        this.cancelOnDistanceExceeded = config.getBoolean("settings.restrictions.cancel-on-distance-exceeded", true);
        this.blockAnvilRenamedItems = config.getBoolean("settings.restrictions.block-anvil-renamed-items", true);

        // Черный список
        this.blacklistedMaterials.clear();
        for (String matName : config.getStringList("settings.blacklist.materials")) {
            try {
                Material mat = Material.matchMaterial(matName.toUpperCase());
                if (mat != null) {
                    this.blacklistedMaterials.add(mat);
                }
            } catch (Exception ignored) {}
        }
        this.blacklistedCmd.clear();
        this.blacklistedCmd.addAll(config.getIntegerList("settings.blacklist.custom-model-data"));
        this.blacklistedTags.clear();
        this.blacklistedTags.addAll(config.getStringList("settings.blacklist.nbt-tags"));

        // Звуки
        this.soundsEnabled = config.getBoolean("sounds.enabled", true);
        this.soundNames.clear();
        if (config.isConfigurationSection("sounds")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("sounds")).getKeys(false)) {
                this.soundNames.put(key, config.getString("sounds." + key));
            }
        }

        // Элементы GUI
        this.guiTitle = ColorUtil.colorize(config.getString("gui.title", "<gradient:#FFA500:#FFD700>Безопасный обмен предметами</gradient>"));
        this.separatorMaterial = parseMaterial(config.getString("gui.separator-material", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE);
        this.fillerMaterial = parseMaterial(config.getString("gui.filler-material", "BLACK_STAINED_GLASS_PANE"), Material.BLACK_STAINED_GLASS_PANE);
        this.notReadyMaterial = parseMaterial(config.getString("gui.ready-buttons.not-ready-material", "RED_STAINED_GLASS_PANE"), Material.RED_STAINED_GLASS_PANE);
        this.readyMaterial = parseMaterial(config.getString("gui.ready-buttons.ready-material", "LIME_STAINED_GLASS_PANE"), Material.LIME_STAINED_GLASS_PANE);
        this.timerWaitingMaterial = parseMaterial(config.getString("gui.timer-indicator.waiting-material", "YELLOW_STAINED_GLASS_PANE"), Material.YELLOW_STAINED_GLASS_PANE);
        this.timerActiveMaterial = parseMaterial(config.getString("gui.timer-indicator.active-material", "CLOCK"), Material.CLOCK);

        // Префикс
        this.rawPrefix = messages.getString("prefix", "<gradient:#FFA500:#FFD700>GlowTrade >></gradient> ");
        this.prefix = ColorUtil.colorize(rawPrefix);
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        Material mat = Material.matchMaterial(name.toUpperCase());
        return mat != null ? mat : fallback;
    }

    /**
     * Получить отформатированное сообщение из messages.yml с подстановкой плейсхолдеров.
     */
    public String getMessage(String path, String... replacements) {
        String msg = messages.getString(path);
        if (msg == null) {
            return "§cMissing message: " + path;
        }

        msg = msg.replace("<prefix>", rawPrefix);

        if (replacements != null && replacements.length >= 2) {
            for (int i = 0; i < replacements.length; i += 2) {
                String target = replacements[i];
                String val = (i + 1 < replacements.length) ? replacements[i + 1] : "";
                msg = msg.replace(target, val);
            }
        }

        return ColorUtil.colorize(msg);
    }

    /**
     * Получить список отформатированных строк (для многострочных сообщений и лора).
     */
    public List<String> getMessageList(String path, String... replacements) {
        List<String> list = messages.getStringList(path);
        List<String> result = new ArrayList<>();

        if (list.isEmpty()) {
            String single = messages.getString(path);
            if (single != null) {
                list = Collections.singletonList(single);
            }
        }

        for (String line : list) {
            line = line.replace("<prefix>", rawPrefix);
            if (replacements != null && replacements.length >= 2) {
                for (int i = 0; i < replacements.length; i += 2) {
                    String target = replacements[i];
                    String val = (i + 1 < replacements.length) ? replacements[i + 1] : "";
                    line = line.replace(target, val);
                }
            }
            result.add(ColorUtil.colorize(line));
        }

        return result;
    }

    /**
     * Отправить сообщение игроку или в консоль.
     */
    public void sendMessage(CommandSender sender, String path, String... replacements) {
        if (sender == null) return;
        List<String> list = messages.getStringList(path);
        if (!list.isEmpty()) {
            for (String line : getMessageList(path, replacements)) {
                sender.sendMessage(line);
            }
        } else {
            String msg = getMessage(path, replacements);
            if (!msg.isEmpty()) {
                sender.sendMessage(msg);
            }
        }
    }

    public String getSoundName(String key) {
        return soundNames.getOrDefault(key, "NONE");
    }

    // Геттеры
    public int getRequestTimeout() { return requestTimeout; }
    public int getCountdownSeconds() { return countdownSeconds; }
    public boolean isDistanceEnabled() { return distanceEnabled; }
    public double getMaxDistance() { return maxDistance; }
    public boolean isRequireSameWorld() { return requireSameWorld; }
    public boolean isShiftClickRequest() { return shiftClickRequest; }
    public boolean isPreventCreative() { return preventCreative; }
    public boolean isPreventSpectator() { return preventSpectator; }
    public boolean isPreventInCombat() { return preventInCombat; }
    public boolean isCancelOnDamage() { return cancelOnDamage; }
    public boolean isCancelOnDistanceExceeded() { return cancelOnDistanceExceeded; }
    public boolean isBlockAnvilRenamedItems() { return blockAnvilRenamedItems; }
    public List<Material> getBlacklistedMaterials() { return blacklistedMaterials; }
    public List<Integer> getBlacklistedCmd() { return blacklistedCmd; }
    public List<String> getBlacklistedTags() { return blacklistedTags; }
    public boolean isSoundsEnabled() { return soundsEnabled; }
    public String getGuiTitle() { return guiTitle; }
    public Material getSeparatorMaterial() { return separatorMaterial; }
    public Material getFillerMaterial() { return fillerMaterial; }
    public Material getNotReadyMaterial() { return notReadyMaterial; }
    public Material getReadyMaterial() { return readyMaterial; }
    public Material getTimerWaitingMaterial() { return timerWaitingMaterial; }
    public Material getTimerActiveMaterial() { return timerActiveMaterial; }
    public String getPrefix() { return prefix; }
}
