package ru.glowdev.glowsnake.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import ru.glowdev.glowsnake.GlowSnakePlugin;
import ru.glowdev.glowsnake.util.ChatUtil;

import java.util.List;
import java.util.Locale;

public class ConfigManager {

    private final GlowSnakePlugin plugin;

    // Game settings
    private long tickDelay;
    private String startDirection;
    private int initialLength;

    // Materials
    private Material separatorMaterial;
    private Material snakeHeadMaterial;
    private Material snakeBodyMaterial;
    private Material appleMaterial;
    private Material joystickMaterial;

    // Controls
    private boolean enableWasd;
    private double sensitivity;
    private boolean enableGuiClicks;
    private boolean enableNumberKeys;

    // Sounds
    private String soundEat;
    private String soundGameOver;
    private String soundGameOverSecondary;
    private String soundClick;
    private float soundVolume;
    private float soundPitchEat;
    private float soundPitchGameOver;

    public ConfigManager(GlowSnakePlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.tickDelay = Math.max(1, config.getLong("game-settings.tick-delay", 18L));
        this.startDirection = config.getString("game-settings.start-direction", "RIGHT").toUpperCase(Locale.ROOT);
        this.initialLength = Math.max(2, config.getInt("game-settings.initial-length", 3));

        this.separatorMaterial = parseMaterial(config.getString("materials.separator", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE);
        this.snakeHeadMaterial = parseMaterial(config.getString("materials.snake-head", "LIME_CONCRETE"), Material.LIME_CONCRETE);
        this.snakeBodyMaterial = parseMaterial(config.getString("materials.snake-body", "GREEN_CONCRETE"), Material.GREEN_CONCRETE);
        this.appleMaterial = parseMaterial(config.getString("materials.apple", "APPLE"), Material.APPLE);
        this.joystickMaterial = parseMaterial(config.getString("materials.joystick-button", "LIME_CONCRETE"), Material.LIME_CONCRETE);

        this.enableWasd = config.getBoolean("controls.enable-wasd", true);
        this.sensitivity = config.getDouble("controls.sensitivity", 0.01);
        this.enableGuiClicks = config.getBoolean("controls.enable-gui-clicks", true);
        this.enableNumberKeys = config.getBoolean("controls.enable-number-keys", true);

        this.soundEat = config.getString("sounds.eat", "ENTITY_EXPERIENCE_ORB_PICKUP");
        this.soundGameOver = config.getString("sounds.game-over", "ENTITY_VILLAGER_NO");
        this.soundGameOverSecondary = config.getString("sounds.game-over-secondary", "BLOCK_ANVIL_LAND");
        this.soundClick = config.getString("sounds.click", "UI_BUTTON_CLICK");
        this.soundVolume = (float) config.getDouble("sounds.volume", 1.0);
        this.soundPitchEat = (float) config.getDouble("sounds.pitch-eat", 1.2);
        this.soundPitchGameOver = (float) config.getDouble("sounds.pitch-game-over", 0.8);
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null || name.trim().isEmpty()) {
            return fallback;
        }
        try {
            Material material = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
            return material != null ? material : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    public String getMessage(String path, String def) {
        String msg = plugin.getConfig().getString("messages." + path, def);
        return ChatUtil.color(msg);
    }

    public String getPrefixedMessage(String path, String def) {
        String prefix = getMessage("prefix", "&2[&aGlowSnake&2] &r");
        return prefix + getMessage(path, def);
    }

    public String getGuiString(String path, String def) {
        String msg = plugin.getConfig().getString("gui." + path, def);
        return ChatUtil.color(msg);
    }

    public List<String> getGuiLore(String path) {
        return ChatUtil.color(plugin.getConfig().getStringList("gui." + path));
    }

    public long getTickDelay() {
        return tickDelay;
    }

    public String getStartDirection() {
        return startDirection;
    }

    public int getInitialLength() {
        return initialLength;
    }

    public Material getSeparatorMaterial() {
        return separatorMaterial;
    }

    public Material getJoystickMaterial() {
        return joystickMaterial;
    }

    public Material getSnakeHeadMaterial() {
        return snakeHeadMaterial;
    }

    public Material getSnakeBodyMaterial() {
        return snakeBodyMaterial;
    }

    public Material getAppleMaterial() {
        return appleMaterial;
    }

    public boolean isEnableWasd() {
        return enableWasd;
    }

    public double getSensitivity() {
        return sensitivity;
    }

    public boolean isEnableGuiClicks() {
        return enableGuiClicks;
    }

    public boolean isEnableNumberKeys() {
        return enableNumberKeys;
    }

    public String getSoundEat() {
        return soundEat;
    }

    public String getSoundGameOver() {
        return soundGameOver;
    }

    public String getSoundGameOverSecondary() {
        return soundGameOverSecondary;
    }

    public String getSoundClick() {
        return soundClick;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitchEat() {
        return soundPitchEat;
    }

    public float getSoundPitchGameOver() {
        return soundPitchGameOver;
    }
}
