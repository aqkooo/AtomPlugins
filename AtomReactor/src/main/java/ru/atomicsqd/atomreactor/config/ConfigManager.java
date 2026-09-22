package ru.atomicsqd.atomreactor.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import ru.atomicsqd.atomreactor.AtomReactor;
import ru.atomicsqd.atomreactor.model.GenerationTarget;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads, validates, and provides typed access to config.yml settings.
 */
public class ConfigManager {

    private final AtomReactor plugin;
    private Material reactorMaterial;
    private GenerationTarget generationTarget;
    private boolean ownerMustBeOnline;
    private final Map<Integer, ReactorLevel> levels = new HashMap<>();

    public ConfigManager(AtomReactor plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        // 1. Reactor Block Material
        String matName = cfg.getString("reactor-block", "CRYING_OBSIDIAN");
        this.reactorMaterial = Material.matchMaterial(matName);
        if (this.reactorMaterial == null) {
            plugin.getLogger().warning("Invalid reactor-block material: " + matName + "! Defaulting to CRYING_OBSIDIAN.");
            this.reactorMaterial = Material.CRYING_OBSIDIAN;
        }

        // 2. Farming settings
        String targetStr = cfg.getString("farming.target", "OWNER_GLOBAL");
        this.generationTarget = GenerationTarget.fromString(targetStr);
        this.ownerMustBeOnline = cfg.getBoolean("farming.owner-must-be-online", true);

        // 3. Levels
        levels.clear();
        ConfigurationSection lvlSec = cfg.getConfigurationSection("levels");
        if (lvlSec != null) {
            for (String key : lvlSec.getKeys(false)) {
                try {
                    int lvl = Integer.parseInt(key);
                    ConfigurationSection sec = lvlSec.getConfigurationSection(key);
                    if (sec != null) {
                        String name = sec.getString("name", "Реактор Ур. " + lvl);
                        double income = sec.getDouble("income", 25.0);
                        int interval = sec.getInt("interval-seconds", 30);
                        int radius = sec.getInt("radius", 10);
                        double cost = sec.getDouble("upgrade-cost", 5000.0);
                        double maxStorage = sec.getDouble("max-storage", 10000.0);
                        String particle = sec.getString("particle", "DUST");
                        String particleColor = sec.getString("particle-color", "#56CCF2");

                        levels.put(lvl, new ReactorLevel(lvl, name, income, interval, radius, cost, maxStorage, particle, particleColor));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // Fallback level 1 if none found
        if (levels.isEmpty()) {
            levels.put(1, new ReactorLevel(1, "<gradient:#56CCF2:#2F80ED>Реактор Ур. I</gradient>", 25.0, 30, 10, 5000.0, 10000.0, "DUST", "#56CCF2"));
        }
    }

    public Material getReactorMaterial() {
        return reactorMaterial;
    }

    public GenerationTarget getGenerationTarget() {
        return generationTarget;
    }

    public boolean isOwnerMustBeOnline() {
        return ownerMustBeOnline;
    }

    public ReactorLevel getLevel(int level) {
        return levels.get(level);
    }

    public ReactorLevel getNextLevel(int currentLevel) {
        return levels.get(currentLevel + 1);
    }

    public int getMaxLevel() {
        return levels.keySet().stream().max(Integer::compareTo).orElse(1);
    }

    public Map<Integer, ReactorLevel> getLevels() {
        return Collections.unmodifiableMap(levels);
    }

    public String getPrefix() {
        return plugin.getConfig().getString("prefix", "");
    }

    public String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    public List<String> getHologramLines() {
        return plugin.getConfig().getStringList("hologram.lines");
    }

    public double getHologramYOffset() {
        return plugin.getConfig().getDouble("hologram.y-offset", 1.7);
    }

    public boolean isHologramEnabled() {
        return plugin.getConfig().getBoolean("hologram.enabled", true);
    }

    public String getCurrencySymbol() {
        return plugin.getConfig().getString("economy.currency-symbol", "$");
    }
}
