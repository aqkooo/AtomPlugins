package ru.atomicsqd.atommessage.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class TabMessage {
    private final String id;
    private final List<String> lines;
    private final int durationSeconds;
    private final String permission;
    private final String sound;

    public TabMessage(@NotNull String id, @NotNull List<String> lines, int durationSeconds,
                      @Nullable String permission, @Nullable String sound) {
        this.id = id;
        this.lines = Collections.unmodifiableList(lines);
        this.durationSeconds = durationSeconds;
        this.permission = permission;
        this.sound = sound;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public List<String> getLines() {
        return lines;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    @Nullable
    public String getPermission() {
        return permission;
    }

    @Nullable
    public String getSound() {
        return sound;
    }

    @NotNull
    public String getJoinedText() {
        return String.join("\n", lines);
    }
}
