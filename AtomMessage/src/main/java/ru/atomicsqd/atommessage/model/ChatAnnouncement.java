package ru.atomicsqd.atommessage.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ChatAnnouncement {
    private final String id;
    private final List<String> lines;
    private final int intervalSeconds;
    private final String permission;
    private final List<String> worlds;
    private final String soundName;
    private final float soundVolume;
    private final float soundPitch;
    private final String title;
    private final String subtitle;
    private final int titleFadeIn;
    private final int titleStay;
    private final int titleFadeOut;
    private final String actionbar;
    private final boolean bossbarEnabled;
    private final String bossbarTitle;
    private final String bossbarColor;
    private final String bossbarOverlay;
    private final int bossbarDuration;

    public ChatAnnouncement(
            @NotNull String id,
            @NotNull List<String> lines,
            int intervalSeconds,
            @Nullable String permission,
            @Nullable List<String> worlds,
            @Nullable String soundName,
            float soundVolume,
            float soundPitch,
            @Nullable String title,
            @Nullable String subtitle,
            int titleFadeIn,
            int titleStay,
            int titleFadeOut,
            @Nullable String actionbar,
            boolean bossbarEnabled,
            @Nullable String bossbarTitle,
            @Nullable String bossbarColor,
            @Nullable String bossbarOverlay,
            int bossbarDuration
    ) {
        this.id = id;
        this.lines = lines;
        this.intervalSeconds = intervalSeconds;
        this.permission = permission;
        this.worlds = worlds != null ? worlds : Collections.emptyList();
        this.soundName = soundName;
        this.soundVolume = soundVolume;
        this.soundPitch = soundPitch;
        this.title = title;
        this.subtitle = subtitle;
        this.titleFadeIn = titleFadeIn;
        this.titleStay = titleStay;
        this.titleFadeOut = titleFadeOut;
        this.actionbar = actionbar;
        this.bossbarEnabled = bossbarEnabled;
        this.bossbarTitle = bossbarTitle;
        this.bossbarColor = bossbarColor;
        this.bossbarOverlay = bossbarOverlay;
        this.bossbarDuration = bossbarDuration;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public List<String> getLines() {
        return lines;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    @Nullable
    public String getPermission() {
        return permission;
    }

    @NotNull
    public List<String> getWorlds() {
        return worlds;
    }

    @Nullable
    public String getSoundName() {
        return soundName;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getSubtitle() {
        return subtitle;
    }

    public int getTitleFadeIn() {
        return titleFadeIn;
    }

    public int getTitleStay() {
        return titleStay;
    }

    public int getTitleFadeOut() {
        return titleFadeOut;
    }

    @Nullable
    public String getActionbar() {
        return actionbar;
    }

    public boolean isBossbarEnabled() {
        return bossbarEnabled;
    }

    @Nullable
    public String getBossbarTitle() {
        return bossbarTitle;
    }

    @Nullable
    public String getBossbarColor() {
        return bossbarColor;
    }

    @Nullable
    public String getBossbarOverlay() {
        return bossbarOverlay;
    }

    public int getBossbarDuration() {
        return bossbarDuration;
    }
}
