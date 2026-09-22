package ru.atomicsqd.atommessage.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.atomicsqd.atommessage.AtomMessage;
import ru.atomicsqd.atommessage.manager.ChatAnnouncementManager;
import ru.atomicsqd.atommessage.manager.TabMessageManager;
import ru.atomicsqd.atommessage.model.TabMessage;
import ru.atomicsqd.atommessage.util.ColorUtil;

public class AtomMessageExpansion extends PlaceholderExpansion {
    private final AtomMessage plugin;

    public AtomMessageExpansion(@NotNull AtomMessage plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "atommessage";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ejyqyl, atomicsqd";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        Player onlinePlayer = player != null && player.isOnline() ? player.getPlayer() : null;
        return resolve(onlinePlayer, params.toLowerCase());
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return resolve(player, params.toLowerCase());
    }

    private String resolve(@Nullable Player player, @NotNull String param) {
        TabMessageManager tabManager = plugin.getTabMessageManager();
        ChatAnnouncementManager chatManager = plugin.getChatAnnouncementManager();

        // TAB Placeholders
        if (param.equals("tab") || param.equals("message") || param.equals("text")) {
            return tabManager.getCurrentFormattedText(player);
        }

        if (param.equals("tab_legacy") || param.equals("legacy")) {
            String raw = tabManager.getCurrentFormattedText(player);
            return ColorUtil.toLegacyString(raw, player);
        }

        if (param.equals("raw") || param.equals("clean")) {
            String raw = tabManager.getCurrentFormattedText(player);
            return ColorUtil.stripColor(raw);
        }

        if (param.equals("id") || param.equals("tab_id")) {
            TabMessage current = tabManager.getCurrentMessage();
            return current != null ? current.getId() : "";
        }

        if (param.equals("index") || param.equals("tab_index")) {
            return String.valueOf(tabManager.getCurrentIndex());
        }

        if (param.equals("total") || param.equals("tab_total")) {
            return String.valueOf(tabManager.getTotalCount());
        }

        if (param.equals("time_left") || param.equals("seconds") || param.equals("tab_time_left")) {
            return String.valueOf(tabManager.getSecondsRemaining());
        }

        // CHAT Placeholders
        if (param.equals("chat_current") || param.equals("chat_id")) {
            return chatManager != null ? chatManager.getLastBroadcastId() : "";
        }

        if (param.equals("chat_next_seconds") || param.equals("chat_time_left")) {
            return chatManager != null ? String.valueOf(chatManager.getSecondsUntilNextBroadcast()) : "0";
        }

        if (param.equals("chat_total")) {
            return chatManager != null ? String.valueOf(chatManager.getTotalAnnouncements()) : "0";
        }

        return null;
    }
}
