package ru.atomicsqd.atommessage.manager;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.atomicsqd.atommessage.config.ConfigManager;
import ru.atomicsqd.atommessage.model.ChatAnnouncement;
import ru.atomicsqd.atommessage.model.RotationOrder;
import ru.atomicsqd.atommessage.util.ColorUtil;

import java.time.Duration;
import java.util.*;

public class ChatAnnouncementManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final Random random = new Random();

    private int currentIndex = 0;
    private int secondsRemaining = 0;
    private String lastBroadcastId = "none";

    private BukkitTask broadcastTask;
    private final List<BossBar> activeBossBars = new ArrayList<>();

    public ChatAnnouncementManager(@NotNull JavaPlugin plugin, @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void start() {
        stop();

        if (!configManager.isChatAnnouncementsEnabled()) {
            return;
        }

        List<ChatAnnouncement> announcements = configManager.getChatAnnouncements();
        if (announcements.isEmpty()) {
            return;
        }

        currentIndex = 0;
        secondsRemaining = announcements.get(0).getIntervalSeconds() > 0
                ? announcements.get(0).getIntervalSeconds()
                : configManager.getChatDefaultInterval();

        broadcastTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSecond, 20L, 20L);
    }

    public void stop() {
        if (broadcastTask != null && !broadcastTask.isCancelled()) {
            broadcastTask.cancel();
            broadcastTask = null;
        }
        clearActiveBossBars();
    }

    private void tickSecond() {
        List<ChatAnnouncement> announcements = configManager.getChatAnnouncements();
        if (announcements.isEmpty()) {
            return;
        }

        secondsRemaining--;
        if (secondsRemaining <= 0) {
            broadcastCurrent();
            advanceNext();
        }
    }

    private void broadcastCurrent() {
        List<ChatAnnouncement> announcements = configManager.getChatAnnouncements();
        if (announcements.isEmpty()) {
            return;
        }

        if (currentIndex < 0 || currentIndex >= announcements.size()) {
            currentIndex = 0;
        }

        ChatAnnouncement announcement = announcements.get(currentIndex);
        broadcast(announcement);
    }

    private void advanceNext() {
        List<ChatAnnouncement> announcements = configManager.getChatAnnouncements();
        if (announcements.isEmpty()) {
            return;
        }

        if (configManager.getChatRotationOrder() == RotationOrder.RANDOM && announcements.size() > 1) {
            int next;
            do {
                next = random.nextInt(announcements.size());
            } while (next == currentIndex);
            currentIndex = next;
        } else {
            currentIndex = (currentIndex + 1) % announcements.size();
        }

        ChatAnnouncement nextAnnouncement = announcements.get(currentIndex);
        secondsRemaining = nextAnnouncement.getIntervalSeconds() > 0
                ? nextAnnouncement.getIntervalSeconds()
                : configManager.getChatDefaultInterval();
    }

    public boolean broadcastById(@NotNull String id, @Nullable CommandSender sender) {
        ChatAnnouncement announcement = configManager.getChatAnnouncement(id);
        if (announcement == null) {
            return false;
        }
        broadcast(announcement);
        return true;
    }

    public void broadcast(@NotNull ChatAnnouncement announcement) {
        lastBroadcastId = announcement.getId();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isEligible(player, announcement)) {
                continue;
            }

            // 1. Send chat message lines
            for (String rawLine : announcement.getLines()) {
                Component comp = ColorUtil.parseComponent(rawLine, player);
                player.sendMessage(comp);
            }

            // 2. Play Sound
            playSound(player, announcement);

            // 3. Send Title / Subtitle if configured
            if (announcement.getTitle() != null || announcement.getSubtitle() != null) {
                Component titleComp = announcement.getTitle() != null
                        ? ColorUtil.parseComponent(announcement.getTitle(), player)
                        : Component.empty();
                Component subtitleComp = announcement.getSubtitle() != null
                        ? ColorUtil.parseComponent(announcement.getSubtitle(), player)
                        : Component.empty();

                Title.Times times = Title.Times.times(
                        Duration.ofMillis(announcement.getTitleFadeIn() * 50L),
                        Duration.ofMillis(announcement.getTitleStay() * 50L),
                        Duration.ofMillis(announcement.getTitleFadeOut() * 50L)
                );
                player.showTitle(Title.title(titleComp, subtitleComp, times));
            }

            // 4. Send ActionBar if configured
            if (announcement.getActionbar() != null && !announcement.getActionbar().isEmpty()) {
                Component actionComp = ColorUtil.parseComponent(announcement.getActionbar(), player);
                player.sendActionBar(actionComp);
            }

            // 5. Send BossBar if configured
            if (announcement.isBossbarEnabled() && announcement.getBossbarTitle() != null) {
                showBossBar(player, announcement);
            }
        }
    }

    private boolean isEligible(@NotNull Player player, @NotNull ChatAnnouncement announcement) {
        // Permission check
        if (announcement.getPermission() != null && !announcement.getPermission().isEmpty()) {
            if (!player.hasPermission(announcement.getPermission())) {
                return false;
            }
        }

        // World check
        if (!announcement.getWorlds().isEmpty()) {
            String worldName = player.getWorld().getName();
            boolean match = false;
            for (String w : announcement.getWorlds()) {
                if (w.equalsIgnoreCase(worldName)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                return false;
            }
        }

        return true;
    }

    private void playSound(@NotNull Player player, @NotNull ChatAnnouncement announcement) {
        String soundName = announcement.getSoundName() != null && !announcement.getSoundName().isEmpty()
                ? announcement.getSoundName()
                : configManager.getChatGlobalSound();

        if (soundName == null || soundName.equalsIgnoreCase("NONE") || soundName.isEmpty()) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            float vol = announcement.getSoundVolume() > 0 ? announcement.getSoundVolume() : configManager.getChatGlobalSoundVolume();
            float pitch = announcement.getSoundPitch() > 0 ? announcement.getSoundPitch() : configManager.getChatGlobalSoundPitch();
            player.playSound(player.getLocation(), sound, vol, pitch);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void showBossBar(@NotNull Player player, @NotNull ChatAnnouncement announcement) {
        try {
            Component title = ColorUtil.parseComponent(announcement.getBossbarTitle(), player);
            BossBar.Color color = parseBossBarColor(announcement.getBossbarColor());
            BossBar.Overlay overlay = parseBossBarOverlay(announcement.getBossbarOverlay());

            BossBar bossBar = BossBar.bossBar(title, 1.0f, color, overlay);
            bossBar.addViewer(player);
            activeBossBars.add(bossBar);

            int durationTicks = Math.max(20, announcement.getBossbarDuration() * 20);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                bossBar.removeViewer(player);
                activeBossBars.remove(bossBar);
            }, durationTicks);
        } catch (Exception ignored) {
        }
    }

    private BossBar.Color parseBossBarColor(@Nullable String name) {
        if (name == null) return BossBar.Color.PURPLE;
        try {
            return BossBar.Color.valueOf(name.toUpperCase());
        } catch (Exception ex) {
            return BossBar.Color.PURPLE;
        }
    }

    private BossBar.Overlay parseBossBarOverlay(@Nullable String name) {
        if (name == null) return BossBar.Overlay.PROGRESS;
        try {
            return BossBar.Overlay.valueOf(name.toUpperCase());
        } catch (Exception ex) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    private void clearActiveBossBars() {
        for (BossBar bossBar : activeBossBars) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                bossBar.removeViewer(player);
            }
        }
        activeBossBars.clear();
    }

    public int getSecondsUntilNextBroadcast() {
        return Math.max(0, secondsRemaining);
    }

    public String getLastBroadcastId() {
        return lastBroadcastId;
    }

    public int getTotalAnnouncements() {
        return configManager.getChatAnnouncements().size();
    }
}
