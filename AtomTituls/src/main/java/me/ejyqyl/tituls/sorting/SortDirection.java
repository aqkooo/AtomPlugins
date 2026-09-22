package me.ejyqyl.tituls.sorting;

public enum SortDirection {
    MAX,
    MIN;

    public static SortDirection fromString(String name) {
        if (name == null) {
            return MAX;
        }
        for (SortDirection dir : values()) {
            if (dir.name().equalsIgnoreCase(name)) {
                return dir;
            }
        }
        return MAX;
    }
}
