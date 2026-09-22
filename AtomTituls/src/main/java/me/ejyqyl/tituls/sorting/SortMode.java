package me.ejyqyl.tituls.sorting;

import org.jetbrains.annotations.NotNull;

public class SortMode {
    private final String name;
    private final SortDirection direction;
    private final String value;
    private final String selectedName;
    private final String unselectedName;
    private final int priority;

    public SortMode(@NotNull String name, @NotNull SortDirection direction, @NotNull String value,
                    @NotNull String selectedName, @NotNull String unselectedName, int priority) {
        this.name = name;
        this.direction = direction;
        this.value = value;
        this.selectedName = selectedName;
        this.unselectedName = unselectedName;
        this.priority = priority;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public SortDirection getDirection() {
        return direction;
    }

    @NotNull
    public String getValue() {
        return value;
    }

    @NotNull
    public String getSelectedName() {
        return selectedName;
    }

    @NotNull
    public String getUnselectedName() {
        return unselectedName;
    }

    public int getPriority() {
        return priority;
    }
}
