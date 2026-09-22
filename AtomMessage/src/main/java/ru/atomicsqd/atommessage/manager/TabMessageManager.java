package ru.atomicsqd.atommessage.manager;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.atomicsqd.atommessage.config.ConfigManager;
import ru.atomicsqd.atommessage.model.RotationOrder;
import ru.atomicsqd.atommessage.model.TabMessage;
import ru.atomicsqd.atommessage.util.ColorUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TabMessageManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final Random random = new Random();

    private int currentIndex = 0;
    private int secondsRemaining = 0;

    private BukkitTask rotationTask;
    private BukkitTask directTabUpdateTask;

    public TabMessageManager(@NotNull JavaPlugin plugin, @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void start() {
        stop();

        List<TabMessage> messages = configManager.getTabMessages();
        if (messages.isEmpty()) {
            return;
        }

        currentIndex = 0;
        secondsRemaining = messages.get(0).getDurationSeconds();

        // Rotation timer task (runs every second = 20 ticks)
        rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSecond, 20L, 20L);

        // Direct TabList updater task (if direct mode is enabled)
        if (configManager.isDirectTabListEnabled()) {
            long interval = configManager.getDirectTabListUpdateTicks();
            directTabUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllDirectTabs, 5L, interval);
        }
    }

    public void stop() {
        if (rotationTask != null && !rotationTask.isCancelled()) {
            rotationTask.cancel();
            rotationTask = null;
        }
        if (directTabUpdateTask != null && !directTabUpdateTask.isCancelled()) {
            directTabUpdateTask.cancel();
            directTabUpdateTask = null;
        }
    }

    private void tickSecond() {
        List<TabMessage> messages = configManager.getTabMessages();
        if (messages.isEmpty()) {
            return;
        }

        secondsRemaining--;
        if (secondsRemaining <= 0) {
            next();
        }
    }

    public void next() {
        List<TabMessage> messages = configManager.getTabMessages();
        if (messages.isEmpty()) {
            return;
        }

        if (configManager.getRotationOrder() == RotationOrder.RANDOM && messages.size() > 1) {
            int nextIdx;
            do {
                nextIdx = random.nextInt(messages.size());
            } while (nextIdx == currentIndex);
            currentIndex = nextIdx;
        } else {
            currentIndex = (currentIndex + 1) % messages.size();
        }

        TabMessage current = messages.get(currentIndex);
        secondsRemaining = current.getDurationSeconds();

        onMessageChanged(current);
    }

    public void prev() {
        List<TabMessage> messages = configManager.getTabMessages();
        if (messages.isEmpty()) {
            return;
        }

        currentIndex = (currentIndex - 1 + messages.size()) % messages.size();
        TabMessage current = messages.get(currentIndex);
        secondsRemaining = current.getDurationSeconds();

        onMessageChanged(current);
    }

    private void onMessageChanged(TabMessage message) {
        // Play sounds if configured
        String soundName = message.getSound() != null ? message.getSound() : configManager.getGlobalSound();
        if (soundName != null && !soundName.equalsIgnoreCase("NONE")) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                float volume = configManager.getSoundVolume();
                float pitch = configManager.getSoundPitch();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (message.getPermission() == null || player.hasPermission(message.getPermission())) {
                        player.playSound(player.getLocation(), sound, volume, pitch);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // If direct tab list is enabled, update all players immediately
        if (configManager.isDirectTabListEnabled()) {
            updateAllDirectTabs();
        }
    }

    @Nullable
    public TabMessage getCurrentMessage() {
        List<TabMessage> messages = configManager.getTabMessages();
        if (messages.isEmpty()) {
            return null;
        }
        if (currentIndex < 0 || currentIndex >= messages.size()) {
            currentIndex = 0;
        }
        return messages.get(currentIndex);
    }

    public int getCurrentIndex() {
        return currentIndex + 1;
    }

    public int getTotalCount() {
        return configManager.getTabMessages().size();
    }

    public int getSecondsRemaining() {
        return Math.max(0, secondsRemaining);
    }

    /**
     * Resolves current message for PlaceholderAPI or player tab.
     */
    @NotNull
    public String getCurrentFormattedText(@Nullable Player player) {
        TabMessage msg = getCurrentMessage();
        if (msg == null) {
            return "";
        }

        if (player != null && msg.getPermission() != null && !player.hasPermission(msg.getPermission())) {
            return "";
        }

        return msg.getJoinedText();
    }

    /**
     * Updates direct tab header and footer for all online players.
     */
    public void updateAllDirectTabs() {
        if (!configManager.isDirectTabListEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendDirectTab(player);
        }
    }

    /**
     * Renders and sends direct TabList Header & Footer to a specific player.
     */
    public void sendDirectTab(@NotNull Player player) {
        if (!configManager.isDirectTabListEnabled() || !player.isOnline()) {
            return;
        }

        String currentMessageText = getCurrentFormattedText(player);

        // Render Header
        List<String> rawHeader = configManager.getDirectHeaderLines();
        Component headerComp = buildSectionComponent(rawHeader, currentMessageText, player);

        // Render Footer
        List<String> rawFooter = configManager.getDirectFooterLines();
        Component footerComp = buildSectionComponent(rawFooter, currentMessageText, player);

        player.sendPlayerListHeaderAndFooter(headerComp, footerComp);
    }

    @NotNull
    private Component buildSectionComponent(@NotNull List<String> lines, @NotNull String messageReplacement, @NotNull Player player) {
        if (lines.isEmpty()) {
            return Component.empty();
        }

        List<Component> lineComponents = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("{atommessage}")) {
                // If this line has {atommessage}, replace it with the message lines
                if (!messageReplacement.isEmpty()) {
                    String[] msgLines = messageReplacement.split("\n");
                    for (String msgLine : msgLines) {
                        String replaced = line.replace("{atommessage}", msgLine);
                        lineComponents.add(ColorUtil.parseComponent(replaced, player));
                    }
                }
            } else {
                lineComponents.add(ColorUtil.parseComponent(line, player));
            }
        }

        Component result = Component.empty();
        for (int i = 0; i < lineComponents.size(); i++) {
            result = result.append(lineComponents.get(i));
            if (i < lineComponents.size() - 1) {
                result = result.append(Component.newline());
            }
        }
        return result;
    }
}
