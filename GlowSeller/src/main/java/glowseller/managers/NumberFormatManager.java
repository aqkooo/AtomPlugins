package glowseller.managers;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NumberFormatManager {
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q"};
    private final DecimalFormat standardFormat;
    private final DecimalFormat compactFormat;

    public NumberFormatManager() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        this.standardFormat = new DecimalFormat("#,###.##", symbols);
        this.compactFormat = new DecimalFormat("0.#", symbols);
    }

    public String formatNumber(long value) {
        return standardFormat.format(value);
    }

    public String formatNumber(double value) {
        return standardFormat.format(value);
    }

    public String formatCompact(double value) {
        if (value < 1000) {
            return standardFormat.format(value);
        }

        int exp = (int) (Math.log10(value) / 3);
        if (exp >= SUFFIXES.length) {
            exp = SUFFIXES.length - 1;
        }

        double scaled = value / Math.pow(1000, exp);
        return compactFormat.format(scaled) + SUFFIXES[exp];
    }
}
