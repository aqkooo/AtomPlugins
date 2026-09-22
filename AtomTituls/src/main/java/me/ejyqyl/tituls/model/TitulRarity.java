package me.ejyqyl.tituls.model;

public enum TitulRarity {
    DEFAULT,
    RARE;

    public static TitulRarity fromString(String name) {
        if (name == null) {
            return DEFAULT;
        }
        for (TitulRarity rarity : values()) {
            if (rarity.name().equalsIgnoreCase(name)) {
                return rarity;
            }
        }
        return DEFAULT;
    }
}
