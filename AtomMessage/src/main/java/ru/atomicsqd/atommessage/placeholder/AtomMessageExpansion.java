package ru.atomicsqd.atommessage.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.atomicsqd.atommessage.AtomMessage;
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
        TabMessageManager manager = plugin.getTabMessageManager();

        if (param.equals("tab") || param.equals("message") || param.equals("text")) {
            return manager.getCurrentFormattedText(player);
        }

        if (param.equals("tab_legacy") || param.equals("legacy")) {
            String raw = manager.getCurrentFormattedText(player);
            return ColorUtil.toLegacyString(raw, player);
        }

        if (param.equals("raw") || param.equals("clean")) {
            String raw = manager.getCurrentFormattedText(player);
            return ColorUtil.stripColor(raw);
        }

        if (param.equals("id")) {
            TabMessage current = manager.getCurrentMessage();
            return current != null ? current.getId() : "";
        }

        if (param.equals("index")) {
            return String.valueOf(manager.getCurrentIndex());
        }

        if (param.equals("total")) {
            return String.valueOf(manager.getTotalCount());
        }

        if (param.equals("time_left") || param.equals("seconds")) {
            return String.valueOf(manager.getSecondsRemaining());
        }

        return null;
    }
}
