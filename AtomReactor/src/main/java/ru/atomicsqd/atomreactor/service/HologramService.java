package ru.atomicsqd.atomreactor.service;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import ru.atomicsqd.atomreactor.AtomReactor;
import ru.atomicsqd.atomreactor.config.ReactorLevel;
import ru.atomicsqd.atomreactor.model.Reactor;
import ru.atomicsqd.atomreactor.util.ColorUtil;

import java.util.List;
import java.util.UUID;

/**
 * Manages floating TextDisplay holograms positioned directly above each reactor block.
 */
public class HologramService {

    private final AtomReactor plugin;

    public HologramService(AtomReactor plugin) {
        this.plugin = plugin;
    }

    public void updateHologram(Reactor reactor) {
        if (!plugin.getConfigManager().isHologramEnabled()) return;

        Location loc = reactor.getLocation();
        if (loc == null || loc.getWorld() == null) return;

        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        if (!loc.getWorld().isChunkLoaded(chunkX, chunkZ)) return;

        Location holoLoc = loc.clone().add(0.5, plugin.getConfigManager().getHologramYOffset(), 0.5);
        TextDisplay display = findOrCreateDisplay(reactor, holoLoc);
        if (display == null || !display.isValid()) return;

        Component text = buildHologramComponent(reactor);
        display.text(text);
    }

    public void removeHologram(Reactor reactor) {
        UUID entityUuid = reactor.getHologramEntityUuid();
        if (entityUuid == null) return;

        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity != null) {
            entity.remove();
        }
        reactor.setHologramEntityUuid(null);
    }

    private TextDisplay findOrCreateDisplay(Reactor reactor, Location loc) {
        UUID entityUuid = reactor.getHologramEntityUuid();
        if (entityUuid != null) {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity instanceof TextDisplay td && td.isValid()) {
                return td;
            }
        }

        // Spawn new TextDisplay
        World world = loc.getWorld();
        if (world == null) return null;

        try {
            TextDisplay display = world.spawn(loc, TextDisplay.class, td -> {
                td.setBillboard(Display.Billboard.CENTER);
                td.setAlignment(TextDisplay.TextAlignment.CENTER);
                td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // Transparent
                td.setShadowed(true);
                td.setPersistent(false); // Cleaned automatically, no ghost entities
            });
            reactor.setHologramEntityUuid(display.getUniqueId());
            return display;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to spawn TextDisplay hologram: " + e.getMessage());
            return null;
        }
    }

    private Component buildHologramComponent(Reactor reactor) {
        ReactorLevel level = plugin.getConfigManager().getLevel(reactor.getLevel());
        String lvlName = level != null ? level.getName() : "Ур. " + reactor.getLevel();
        double income = level != null ? level.getIncome() : 0.0;
        int interval = level != null ? level.getIntervalSeconds() : 30;

        Player owner = Bukkit.getPlayer(reactor.getOwnerUuid());
        boolean isOwnerOnline = owner != null && owner.isOnline();

        String statusStr;
        if (!reactor.isActive()) {
            statusStr = "<gradient:#FF512F:#DD2476>● Приостановлен</gradient>";
        } else if (plugin.getConfigManager().isOwnerMustBeOnline() && !isOwnerOnline) {
            statusStr = "<gradient:#F3904F:#3B4371>● Владелец не в сети</gradient>";
        } else {
            statusStr = "<gradient:#42E695:#3BB78F>● Активен | В работе</gradient>";
        }

        List<String> rawLines = plugin.getConfigManager().getHologramLines();
        Component combined = Component.empty();

        for (int i = 0; i < rawLines.size(); i++) {
            String line = rawLines.get(i)
                    .replace("%level_name%", lvlName)
                    .replace("%level%", String.valueOf(reactor.getLevel()))
                    .replace("%owner%", reactor.getOwnerName())
                    .replace("%income%", String.format("%.1f", income))
                    .replace("%interval%", String.valueOf(interval))
                    .replace("%currency%", plugin.getConfigManager().getCurrencySymbol())
                    .replace("%status%", statusStr)
                    .replace("%stored%", String.format("%.1f", reactor.getStoredBalance()));

            Component lineComp = ColorUtil.component(line);
            if (i > 0) {
                combined = combined.append(Component.newline());
            }
            combined = combined.append(lineComp);
        }

        return combined;
    }
}
