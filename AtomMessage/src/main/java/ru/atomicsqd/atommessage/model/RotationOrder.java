package ru.atomicsqd.atommessage.model;

public enum RotationOrder {
    SEQUENTIAL,
    RANDOM;

    public static RotationOrder fromString(String name) {
        if (name == null) return SEQUENTIAL;
        for (RotationOrder order : values()) {
            if (order.name().equalsIgnoreCase(name)) {
                return order;
            }
        }
        return SEQUENTIAL;
    }
}
