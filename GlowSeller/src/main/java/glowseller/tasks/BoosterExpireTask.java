package glowseller.tasks;

import glowseller.Main;
import glowseller.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BoosterExpireTask extends BukkitRunnable {
    private final Main plugin;

    public BoosterExpireTask(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getPlayerDataCache().get(player.getUniqueId());
            if (data == null) continue;

            if (data.getActiveBoosterKey() != null && data.getBoosterExpireAt() > 0 && data.getBoosterExpireAt() <= now) {
                // Booster expired!
                data.clearBooster();

                if (plugin.getMainConfig().isActionbarOnBoosterExpire()) {
                    plugin.getMessageConfig().sendActionBar(player, "booster.expired_actionbar");
                }
                plugin.getMessageConfig().send(player, "booster.expired");

                String expireSound = plugin.getMainConfig().getExpireSound();
                try {
                    Sound sound = Sound.valueOf(expireSound.toUpperCase());
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                } catch (Exception ignored) {}

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getPlayerRepository().save(data);
                });
            }
        }
    }
}
