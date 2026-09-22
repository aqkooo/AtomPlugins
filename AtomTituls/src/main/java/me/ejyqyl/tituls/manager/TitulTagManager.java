package me.ejyqyl.tituls.manager;

import me.ejyqyl.tituls.AtomTitulsPlugin;
import me.ejyqyl.tituls.model.Titul;
import me.ejyqyl.tituls.model.TitulRarity;
import me.ejyqyl.tituls.model.TitulType;
import me.ejyqyl.tituls.util.TextUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TitulTagManager {
    private final AtomTitulsPlugin plugin;
    private final NamespacedKey keyId;
    private final NamespacedKey keyType;
    private final NamespacedKey keyName;
    private final NamespacedKey keyTime;

    // Legacy fallback keys
    private final NamespacedKey legacyKeyId;
    private final NamespacedKey legacyKeyType;
    private final NamespacedKey legacyKeyName;
    private final NamespacedKey legacyKeyTime;

    public TitulTagManager(@NotNull AtomTitulsPlugin plugin) {
        this.plugin = plugin;
        this.keyId = new NamespacedKey(plugin, "id");
        this.keyType = new NamespacedKey(plugin, "type");
        this.keyName = new NamespacedKey(plugin, "name");
        this.keyTime = new NamespacedKey(plugin, "time");

        this.legacyKeyId = new NamespacedKey("stickhwtituls", "titul_tag_id");
        this.legacyKeyType = new NamespacedKey("stickhwtituls", "titul_tag_type");
        this.legacyKeyName = new NamespacedKey("stickhwtituls", "titul_tag_name");
        this.legacyKeyTime = new NamespacedKey("stickhwtituls", "titul_tag_time");
    }

    @NotNull
    public ItemStack createTagItem(@NotNull Titul titul) {
        ConfigManager cfg = plugin.getConfigManager();
        String rawName = cfg.getTagName();
        String titulDisplayName = titul.getName();
        String formattedName = rawName.replace("{titul}", titulDisplayName);

        String typeDisplay = titul.getType() == TitulType.CASE
                ? cfg.getMenuConfig().getString("menu.settings.type.case", "Кейсовый")
                : cfg.getMenuConfig().getString("menu.settings.type.custom", "Уникальный");

        List<String> formattedLore = new ArrayList<>();
        for (String line : cfg.getTagLore()) {
            String replaced = line
                    .replace("{type}", typeDisplay)
                    .replace("{time}", titul.getFormatted());
            formattedLore.add(replaced);
        }

        ItemStack item = TextUtil.createItem(cfg.getTagMaterial(), formattedName, formattedLore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(keyId, PersistentDataType.STRING, titul.getId());
            pdc.set(keyType, PersistentDataType.STRING, titul.getType().name());
            pdc.set(keyName, PersistentDataType.STRING, titulDisplayName);
            if (titul.getObtainedAt() != null) {
                pdc.set(keyTime, PersistentDataType.LONG, titul.getObtainedAt());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isTitulTag(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(keyId, PersistentDataType.STRING) || pdc.has(legacyKeyId, PersistentDataType.STRING);
    }

    @Nullable
    public Titul readTitulFromTag(@Nullable ItemStack item) {
        if (!isTitulTag(item)) {
            return null;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(keyId, PersistentDataType.STRING);
        if (id == null) {
            id = pdc.get(legacyKeyId, PersistentDataType.STRING);
        }
        if (id == null) {
            return null;
        }

        String typeRaw = pdc.get(keyType, PersistentDataType.STRING);
        if (typeRaw == null) {
            typeRaw = pdc.get(legacyKeyType, PersistentDataType.STRING);
        }
        TitulType type = TitulType.fromString(typeRaw);

        Long obtainedAt = null;
        if (pdc.has(keyTime, PersistentDataType.LONG)) {
            obtainedAt = pdc.get(keyTime, PersistentDataType.LONG);
        } else if (pdc.has(legacyKeyTime, PersistentDataType.LONG)) {
            obtainedAt = pdc.get(legacyKeyTime, PersistentDataType.LONG);
        }

        if (type == TitulType.CASE) {
            Titul template = plugin.getTitulManager().getTitul(id);
            if (template != null) {
                return new Titul(template.getId(), template.getName(), TitulType.CASE, template.getRarity(), obtainedAt);
            }
        }

        String name = pdc.get(keyName, PersistentDataType.STRING);
        if (name == null) {
            name = pdc.get(legacyKeyName, PersistentDataType.STRING);
        }
        if (name == null) {
            name = id;
        }

        return new Titul(id, name, TitulType.CUSTOM, TitulRarity.RARE, obtainedAt);
    }
}
