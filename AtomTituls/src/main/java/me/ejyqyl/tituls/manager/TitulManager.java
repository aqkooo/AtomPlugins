package me.ejyqyl.tituls.manager;

import me.ejyqyl.tituls.model.Titul;
import me.ejyqyl.tituls.model.TitulRarity;
import me.ejyqyl.tituls.model.TitulType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TitulManager {
    private final Map<String, Titul> caseTitles = new LinkedHashMap<>();

    public void loadFromConfig(@Nullable ConfigurationSection section) {
        caseTitles.clear();
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            String name = section.getString(key + ".name", key);
            String rarityStr = section.getString(key + ".rarity", "DEFAULT");
            TitulRarity rarity = TitulRarity.fromString(rarityStr);

            Titul titul = new Titul(key, name, TitulType.CASE, rarity, null);
            caseTitles.put(key, titul);
        }
    }

    @Nullable
    public Titul getTitul(@NotNull String id) {
        return caseTitles.get(id);
    }

    public boolean hasTitul(@NotNull String id) {
        return caseTitles.containsKey(id);
    }

    @NotNull
    public Collection<Titul> getAllTituls() {
        return Collections.unmodifiableCollection(caseTitles.values());
    }

    public int getCount() {
        return caseTitles.size();
    }
}
