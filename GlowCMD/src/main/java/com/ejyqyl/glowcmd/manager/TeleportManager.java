package com.ejyqyl.glowcmd.manager;

import com.ejyqyl.glowcmd.GlowCMD;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player teleport histories (/back) and pending TPA / TPAHere requests.
 *
 * @author ejyqyl
 */
public final class TeleportManager {

    public static final long TPA_TIMEOUT_MILLIS = 60_000L; // 60 seconds

    public record TpaRequest(UUID senderId, String senderName, UUID targetId, boolean isHere, long timestamp) {
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TPA_TIMEOUT_MILLIS;
        }
    }

    private final GlowCMD plugin;
    private final Map<UUID, Location> backLocations = new ConcurrentHashMap<>();
    private final Map<UUID, TpaRequest> outgoingRequests = new ConcurrentHashMap<>(); // sender -> request
    private final Map<UUID, Map<UUID, TpaRequest>> incomingRequests = new ConcurrentHashMap<>(); // target -> (sender -> request)

    public TeleportManager(@NotNull GlowCMD plugin) {
        this.plugin = plugin;
    }

    public void setLastLocation(@NotNull UUID playerId, @NotNull Location location) {
        this.backLocations.put(playerId, location.clone());
    }

    @Nullable
    public Location getLastLocation(@NotNull UUID playerId) {
        return this.backLocations.get(playerId);
    }

    public void removeLastLocation(@NotNull UUID playerId) {
        this.backLocations.remove(playerId);
    }

    public void sendRequest(@NotNull Player sender, @NotNull Player target, boolean isHere) {
        UUID senderId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();

        // Cancel any previous outgoing request by this sender
        cancelOutgoingRequest(senderId);

        TpaRequest request = new TpaRequest(senderId, sender.getName(), targetId, isHere, System.currentTimeMillis());
        outgoingRequests.put(senderId, request);
        incomingRequests.computeIfAbsent(targetId, k -> new ConcurrentHashMap<>()).put(senderId, request);
    }

    @Nullable
    public TpaRequest getLatestIncomingRequest(@NotNull UUID targetId) {
        Map<UUID, TpaRequest> map = incomingRequests.get(targetId);
        if (map == null || map.isEmpty()) {
            return null;
        }

        // Clean expired requests and find newest valid one
        TpaRequest newest = null;
        for (var it = map.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            if (entry.getValue().isExpired()) {
                outgoingRequests.remove(entry.getKey());
                it.remove();
            } else if (newest == null || entry.getValue().timestamp() > newest.timestamp()) {
                newest = entry.getValue();
            }
        }
        return newest;
    }

    @Nullable
    public TpaRequest getIncomingRequestFrom(@NotNull UUID targetId, @NotNull UUID senderId) {
        Map<UUID, TpaRequest> map = incomingRequests.get(targetId);
        if (map == null) {
            return null;
        }
        TpaRequest request = map.get(senderId);
        if (request != null) {
            if (request.isExpired()) {
                map.remove(senderId);
                outgoingRequests.remove(senderId);
                return null;
            }
            return request;
        }
        return null;
    }

    @Nullable
    public TpaRequest cancelOutgoingRequest(@NotNull UUID senderId) {
        TpaRequest request = outgoingRequests.remove(senderId);
        if (request != null) {
            Map<UUID, TpaRequest> map = incomingRequests.get(request.targetId());
            if (map != null) {
                map.remove(senderId);
            }
        }
        return request;
    }

    public void removeRequest(@NotNull TpaRequest request) {
        outgoingRequests.remove(request.senderId(), request);
        Map<UUID, TpaRequest> map = incomingRequests.get(request.targetId());
        if (map != null) {
            map.remove(request.senderId(), request);
        }
    }

    public void removeAllForPlayer(@NotNull UUID playerId) {
        cancelOutgoingRequest(playerId);
        Map<UUID, TpaRequest> incoming = incomingRequests.remove(playerId);
        if (incoming != null) {
            for (UUID senderId : incoming.keySet()) {
                outgoingRequests.remove(senderId);
            }
        }
        backLocations.remove(playerId);
    }
}
