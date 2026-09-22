package me.ejyqyl.tituls.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class DateUtil {
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("dd.MM.yyyy");
    private static final String UNKNOWN = "-";

    private DateUtil() {
    }

    public static synchronized String format(Long millis) {
        if (millis == null || millis <= 0) {
            return UNKNOWN;
        }
        return FORMAT.format(new Date(millis));
    }
}
