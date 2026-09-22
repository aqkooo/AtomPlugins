package ru.atomicsqd.atomreactor.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.atomicsqd.atomreactor.AtomReactor;
import ru.atomicsqd.atomreactor.config.ReactorLevel;
import ru.atomicsqd.atomreactor.model.Reactor;
import ru.atomicsqd.atomreactor.util.ColorUtil;
import ru.atomicsqd.atomreactor.util.ItemBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all reactor instances, persistent JSON storage, and reactor items.
 */
public class ReactorManager {

    private final AtomReactor plugin;
    private final HologramService hologramService;
    private final File dataFile;
    private final Gson gson;

    private final NamespacedKey reactorItemKey;
    private final NamespacedKey reactorLevelKey;

    private final Map<UUID, Reactor> reactorsById = new ConcurrentHashMap<>();
    private final Map<String, Reactor> reactorsByLoc = new ConcurrentHashMap<>();

    public ReactorManager(AtomReactor plugin, HologramService hologramService) {
        this.plugin = plugin;
        this.hologramService = hologramService;
        this.dataFile = new File(plugin.getDataFolder(), "reactors.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.reactorItemKey = new NamespacedKey(plugin, "reactor_item");
        this.reactorLevelKey = new NamespacedKey(plugin, "reactor_level");
    }

    public void loadReactors() {
        reactorsById.clear();
        reactorsByLoc.clear();

        if (!dataFile.exists()) return;

        try (FileReader reader = new FileReader(dataFile)) {
            Type listType = new TypeToken<List<Reactor>>(){}.getType();
            List<Reactor> loaded = gson.fromJson(reader, listType);
            if (loaded != null) {
                for (Reactor r : loaded) {
                    reactorsById.put(r.getId(), r);
                    reactorsByLoc.put(locKey(r.getWorldName(), r.getX(), r.getY(), r.getZ()), r);
                }
            }
            plugin.getLogger().info("Successfully loaded " + reactorsById.size() + " active reactor(s).");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load reactors.json", e);
        }
    }

    public void saveReactors() {
        if (!dataFile.getParentFile().exists()) {
            dataFile.getParentFile().mkdirs();
        }

        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(new ArrayList<>(reactorsById.values()), writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save reactors.json", e);
        }
    }

    public Reactor createReactor(Player owner, Location loc, int level) {
        Reactor reactor = new Reactor(
                UUID.randomUUID(),
                owner.getUniqueId(),
                owner.getName(),
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ(),
                level
        );

        reactorsById.put(reactor.getId(), reactor);
        reactorsByLoc.put(locKey(reactor.getWorldName(), reactor.getX(), reactor.getY(), reactor.getZ()), reactor);

        saveReactors();
        hologramService.updateHologram(reactor);
        return reactor;
    }

    public void removeReactor(Reactor reactor) {
        if (reactor == null) return;
        hologramService.removeHologram(reactor);
        reactorsById.remove(reactor.getId());
        reactorsByLoc.remove(locKey(reactor.getWorldName(), reactor.getX(), reactor.getY(), reactor.getZ()));
        saveReactors();
    }

    public Reactor getReactorAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return reactorsByLoc.get(locKey(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    public Collection<Reactor> getAllReactors() {
        return Collections.unmodifiableCollection(reactorsById.values());
    }

    public ItemStack createReactorItem(int level) {
        ReactorLevel lvl = plugin.getConfigManager().getLevel(level);
        String nameStr = "<gradient:#FF5F6D:#FFC371>&lАтомный Реактор [Ур. " + level + "]</gradient>";

        double income = lvl != null ? lvl.getIncome() : 25.0;
        int interval = lvl != null ? lvl.getIntervalSeconds() : 30;
        int radius = lvl != null ? lvl.getRadius() : 10;
        String cur = plugin.getConfigManager().getCurrencySymbol();

        List<String> lore = List.of(
                "<gradient:#56CCF2:#2F80ED>● Уровень:</gradient> " + (lvl != null ? lvl.getName() : "Ур. " + level),
                "<gradient:#42E695:#3BB78F>● Доход:</gradient> &f+&a" + income + cur + " &7/ " + interval + "с",
                "<gradient:#FCE38A:#F38181>● Радиус действия:</gradient> &f" + radius + " блоков",
                "",
                "<gradient:#FFC371:#FF5F6D>Установите блок, чтобы начать выработку энергии!</gradient>"
        );

        return new ItemBuilder(plugin.getConfigManager().getReactorMaterial())
                .name(nameStr)
                .lore(lore)
                .pdc(reactorItemKey, (byte) 1)
                .pdc(reactorLevelKey, level)
                .build();
    }

    public boolean isReactorItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(reactorItemKey);
    }

    public int getReactorItemLevel(ItemStack item) {
        if (!isReactorItem(item)) return 1;
        Integer lvl = item.getItemMeta().getPersistentDataContainer().get(reactorLevelKey, org.bukkit.persistence.PersistentDataType.INTEGER);
        return lvl != null ? Math.max(1, lvl) : 1;
    }

    public NamespacedKey getReactorItemKey() {
        return reactorItemKey;
    }

    public NamespacedKey getReactorLevelKey() {
        return reactorLevelKey;
    }

    private String locKey(String world, int x, int y, int z) {
        return world + ":" + x + "," + y + "," + z;
    }
}
