package me.ejyqyl.tituls.model;

public enum TitulType {
    CASE,
    CUSTOM;

    public static TitulType fromString(String name) {
        if (name == null) {
            return CASE;
        }
        for (TitulType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return CASE;
    }
}
