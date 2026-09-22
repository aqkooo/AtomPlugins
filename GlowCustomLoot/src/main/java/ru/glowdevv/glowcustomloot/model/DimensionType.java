package ru.glowdevv.glowcustomloot.model;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum DimensionType {
    OVERWORLD("overworld", "Верхний мир", Material.GRASS_BLOCK, 11),
    NETHER("nether", "Нижний мир", Material.NETHERRACK, 13),
    THE_END("the_end", "Эндер-мир", Material.END_STONE, 15);

    private final String id;
    private final String displayName;
    private final Material defaultIcon;
    private final int defaultSlot;

    DimensionType(String id, String displayName, Material defaultIcon, int defaultSlot) {
        this.id = id;
        this.displayName = displayName;
        this.defaultIcon = defaultIcon;
        this.defaultSlot = defaultSlot;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public Material getDefaultIcon() {
        return defaultIcon;
    }

    public int getDefaultSlot() {
        return defaultSlot;
    }

    @Nullable
    public static DimensionType fromString(@Nullable String input) {
        if (input == null) return null;
        for (DimensionType type : values()) {
            if (type.id.equalsIgnoreCase(input) || type.name().equalsIgnoreCase(input)) {
                return type;
            }
        }
        if (input.equalsIgnoreCase("end")) return THE_END;
        if (input.equalsIgnoreCase("the_nether")) return NETHER;
        if (input.equalsIgnoreCase("world") || input.equalsIgnoreCase("normal")) return OVERWORLD;
        return null;
    }
}
