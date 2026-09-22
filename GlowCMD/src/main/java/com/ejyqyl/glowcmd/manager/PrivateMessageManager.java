package com.ejyqyl.glowcmd.manager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks conversation partners for /msg and /reply commands.
 *
 * @author ejyqyl
 */
public final class PrivateMessageManager {

    private final Map<UUID, UUID> replyTargets = new ConcurrentHashMap<>();

    public void setReplyTarget(@NotNull UUID player1, @NotNull UUID player2) {
        replyTargets.put(player1, player2);
        replyTargets.put(player2, player1);
    }

    @Nullable
    public UUID getReplyTarget(@NotNull UUID player) {
        return replyTargets.get(player);
    }

    public void removePlayer(@NotNull UUID player) {
        UUID partner = replyTargets.remove(player);
        if (partner != null) {
            replyTargets.remove(partner, player);
        }
    }
}
