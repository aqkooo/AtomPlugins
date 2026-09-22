package ru.glowdev.glowsnake.game;

public enum Direction {
    UP(-9, "▲", "Вверх"),
    DOWN(9, "▼", "Вниз"),
    LEFT(-1, "◄", "Влево"),
    RIGHT(1, "►", "Вправо");

    private final int slotOffset;
    private final String symbol;
    private final String displayName;

    Direction(int slotOffset, String symbol, String displayName) {
        this.slotOffset = slotOffset;
        this.symbol = symbol;
        this.displayName = displayName;
    }

    public int getSlotOffset() {
        return slotOffset;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isOpposite(Direction other) {
        if (other == null) return false;
        return (this == UP && other == DOWN)
                || (this == DOWN && other == UP)
                || (this == LEFT && other == RIGHT)
                || (this == RIGHT && other == LEFT);
    }

    public static Direction fromString(String str) {
        if (str == null) return UP;
        try {
            return Direction.valueOf(str.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UP;
        }
    }
}
