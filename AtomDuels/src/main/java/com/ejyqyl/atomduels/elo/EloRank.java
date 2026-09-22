package com.ejyqyl.atomduels.elo;

/**
 * Elo Rank brackets with colors and display names.
 * Authored by ejyqyl.
 */
public enum EloRank {
    BRONZE("Бронза", "#CD7F32", 0, 999, "🥉"),
    SILVER("Серебро", "#C0C0C0", 1000, 1199, "🥈"),
    GOLD("Золото", "#FFD700", 1200, 1399, "🥇"),
    PLATINUM("Платина", "#00CED1", 1400, 1599, "💠"),
    DIAMOND("Алмаз", "#00B5FD", 1600, 1799, "💎"),
    MASTER("Мастер", "#A335EE", 1800, 1999, "🔮"),
    LEGEND("Легенда", "#FF4500", 2000, Integer.MAX_VALUE, "👑");

    private final String displayName;
    private final String hexColor;
    private final int minElo;
    private final int maxElo;
    private final String symbol;

    EloRank(String displayName, String hexColor, int minElo, int maxElo, String symbol) {
        this.displayName = displayName;
        this.hexColor = hexColor;
        this.minElo = minElo;
        this.maxElo = maxElo;
        this.symbol = symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHexColor() {
        return hexColor;
    }

    public int getMinElo() {
        return minElo;
    }

    public int getMaxElo() {
        return maxElo;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getFormattedName() {
        return "<" + hexColor + ">" + symbol + " " + displayName + "</" + hexColor + ">";
    }

    public static EloRank fromElo(int elo) {
        for (EloRank rank : values()) {
            if (elo >= rank.minElo && elo <= rank.maxElo) {
                return rank;
            }
        }
        return elo < 0 ? BRONZE : LEGEND;
    }
}
