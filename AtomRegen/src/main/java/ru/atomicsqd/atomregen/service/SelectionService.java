package ru.atomicsqd.atomregen.service;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import ru.atomicsqd.atomregen.AtomRegen;
import ru.atomicsqd.atomregen.model.BlockVector;
import ru.atomicsqd.atomregen.util.ColorUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player point selections (pos1, pos2), the wand item, and visual particle bounds.
 */
public class SelectionService {

    private final AtomRegen plugin;
    private final NamespacedKey wandKey;

    private final Map<UUID, Location> pos1Map = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pos2Map = new ConcurrentHashMap<>();

    public SelectionService(AtomRegen plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, "selection_wand");
    }

    public ItemStack createWand() {
        String matName = plugin.getConfig().getString("wand.material", "GOLDEN_AXE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.GOLDEN_AXE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = plugin.getConfig().getString("wand.name", "<gradient:#FF5F6D:#FFC371>&lAtomRegen Wand</gradient>");
            meta.setDisplayName(ColorUtil.color(name));

            List<String> lore = plugin.getConfig().getStringList("wand.lore");
            if (lore.isEmpty()) {
                lore = List.of(
                        "&7ЛКМ по блоку &8— &eУстановить Точку 1",
                        "&7ПКМ по блоку &8— &bУстановить Точку 2",
                        "",
                        "&8Команда: &e/ar create <имя> [время_сек]"
                );
            }
            meta.setLore(ColorUtil.color(lore));
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    public void setPos1(Player player, Location loc) {
        pos1Map.put(player.getUniqueId(), loc.clone());
        spawnSelectionParticle(loc, Color.fromRGB(85, 255, 85)); // Green
        highlightSelection(player);
    }

    public void setPos2(Player player, Location loc) {
        pos2Map.put(player.getUniqueId(), loc.clone());
        spawnSelectionParticle(loc, Color.fromRGB(85, 255, 255)); // Cyan
        highlightSelection(player);
    }

    public Location getPos1(UUID uuid) {
        return pos1Map.get(uuid);
    }

    public Location getPos2(UUID uuid) {
        return pos2Map.get(uuid);
    }

    public boolean hasBothPositions(UUID uuid) {
        Location p1 = pos1Map.get(uuid);
        Location p2 = pos2Map.get(uuid);
        return p1 != null && p2 != null && p1.getWorld() != null && p1.getWorld().equals(p2.getWorld());
    }

    public void clearSelection(UUID uuid) {
        pos1Map.remove(uuid);
        pos2Map.remove(uuid);
    }

    public int getSelectionBlockCount(UUID uuid) {
        Location p1 = pos1Map.get(uuid);
        Location p2 = pos2Map.get(uuid);
        if (p1 == null || p2 == null || !p1.getWorld().equals(p2.getWorld())) {
            return 0;
        }
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    private void spawnSelectionParticle(Location loc, Color color) {
        World world = loc.getWorld();
        if (world == null) return;
        Location center = loc.clone().add(0.5, 0.5, 0.5);
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.5f);
        world.spawnParticle(Particle.DUST, center, 15, 0.3, 0.3, 0.3, dust);
        world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.8f);
    }

    /**
     * Visualizes the wireframe bounding box if both positions are selected.
     */
    public void highlightSelection(Player player) {
        Location p1 = pos1Map.get(player.getUniqueId());
        Location p2 = pos2Map.get(player.getUniqueId());
        if (p1 == null || p2 == null || !p1.getWorld().equals(p2.getWorld())) return;

        World world = p1.getWorld();
        double minX = Math.min(p1.getBlockX(), p2.getBlockX());
        double maxX = Math.max(p1.getBlockX(), p2.getBlockX()) + 1.0;
        double minY = Math.min(p1.getBlockY(), p2.getBlockY());
        double maxY = Math.max(p1.getBlockY(), p2.getBlockY()) + 1.0;
        double minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        double maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ()) + 1.0;

        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 170, 0), 1.0f);

        // Draw outline points
        drawBoundingBox(player, world, minX, minY, minZ, maxX, maxY, maxZ, dust);
    }

    private void drawBoundingBox(Player player, World world,
                                 double minX, double minY, double minZ,
                                 double maxX, double maxY, double maxZ,
                                 Particle.DustOptions dust) {
        double step = 1.0;

        // X-axis parallel lines
        for (double x = minX; x <= maxX; x += step) {
            player.spawnParticle(Particle.DUST, x, minY, minZ, 1, dust);
            player.spawnParticle(Particle.DUST, x, maxY, minZ, 1, dust);
            player.spawnParticle(Particle.DUST, x, minY, maxZ, 1, dust);
            player.spawnParticle(Particle.DUST, x, maxY, maxZ, 1, dust);
        }

        // Y-axis parallel lines
        for (double y = minY; y <= maxY; y += step) {
            player.spawnParticle(Particle.DUST, minX, y, minZ, 1, dust);
            player.spawnParticle(Particle.DUST, maxX, y, minZ, 1, dust);
            player.spawnParticle(Particle.DUST, minX, y, maxZ, 1, dust);
            player.spawnParticle(Particle.DUST, maxX, y, maxZ, 1, dust);
        }

        // Z-axis parallel lines
        for (double z = minZ; z <= maxZ; z += step) {
            player.spawnParticle(Particle.DUST, minX, minY, z, 1, dust);
            player.spawnParticle(Particle.DUST, maxX, minY, z, 1, dust);
            player.spawnParticle(Particle.DUST, minX, maxY, z, 1, dust);
            player.spawnParticle(Particle.DUST, maxX, maxY, z, 1, dust);
        }
    }
}
