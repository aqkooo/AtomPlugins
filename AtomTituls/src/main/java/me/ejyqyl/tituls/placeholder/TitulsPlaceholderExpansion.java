package me.ejyqyl.tituls.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.ejyqyl.tituls.AtomTitulsPlugin;
import me.ejyqyl.tituls.manager.ConfigManager;
import me.ejyqyl.tituls.model.Titul;
import me.ejyqyl.tituls.util.TextUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TitulsPlaceholderExpansion extends PlaceholderExpansion {
    private final AtomTitulsPlugin plugin;
    private final String identifier;

    public TitulsPlaceholderExpansion(@NotNull AtomTitulsPlugin plugin, @NotNull String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;
    }

    @Override
    public @NotNull String getIdentifier() {
        return identifier;
    }

    @Override
    public @NotNull String getAuthor() {
        return "ejyqyl";
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
        return resolve(player != null ? player.getUniqueId() : null, params);
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return resolve(player != null ? player.getUniqueId() : null, params);
    }

    private String resolve(@Nullable UUID uuid, @NotNull String params) {
        if (uuid == null) {
            return "";
        }

        ConfigManager cfg = plugin.getConfigManager();
        Titul active = plugin.getPlayerCache().getActiveTitul(uuid);

        String paramLower = params.toLowerCase();

        if (paramLower.equals("tab")) {
            String format = active != null ? cfg.getTagYesTab() : cfg.getTagNoTab();
            if (active != null) {
                format = format.replace("{titul}", active.getName());
            }
            return TextUtil.toLegacy(format);
        }

        if (paramLower.equals("board")) {
            String format = active != null ? cfg.getTagYesBoard() : cfg.getTagNoBoard();
            if (active != null) {
                format = format.replace("{titul}", active.getName());
            }
            return TextUtil.toLegacy(format);
        }

        if (paramLower.equals("active")) {
            return active != null ? TextUtil.toLegacy(active.getName()) : "";
        }

        if (paramLower.equals("active_raw")) {
            return active != null ? TextUtil.stripFormatting(active.getName()) : "";
        }

        if (paramLower.equals("count")) {
            return String.valueOf(plugin.getPlayerCache().getUnlockedTituls(uuid).size());
        }

        if (paramLower.startsWith("has_")) {
            String titleId = params.substring(4);
            return String.valueOf(plugin.getPlayerCache().hasTitul(uuid, titleId));
        }

        return null;
    }
}
