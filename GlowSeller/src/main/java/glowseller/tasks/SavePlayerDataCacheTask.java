package glowseller.tasks;

import glowseller.Main;
import glowseller.models.PlayerData;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class SavePlayerDataCacheTask extends BukkitRunnable {
    private final Main plugin;

    public SavePlayerDataCacheTask(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Collection<PlayerData> dirty = plugin.getPlayerDataCache().getDirtyData();
        if (!dirty.isEmpty()) {
            plugin.getPlayerRepository().saveAll(dirty);
        }
    }
}
