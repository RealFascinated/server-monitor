package cc.fascinated.monitor.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@UtilityClass
public class NumberUtils {
    private static final String[] BYTE_UNITS = {"B", "KB", "MB", "GB", "TB", "PB"};

    @Nullable
    public static Long toLong(@Nullable Double value) {
        return value != null ? value.longValue() : null;
    }

    /** Rounds to at most {@code maxDecimals} places and drops trailing zeros ({@code 90.00} becomes {@code 90}). */
    public static String format(double value, int maxDecimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }
        String text = String.format(Locale.ROOT, "%." + maxDecimals + "f", value);
        if (text.indexOf('.') >= 0) {
            text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return text.equals("-0") ? "0" : text;
    }

    /** Binary-prefixed size such as {@code 1.5 KB}. */
    public static String formatBytes(double value) {
        double scaled = Math.abs(value);
        int unit = 0;
        while (scaled >= 1024 && unit < BYTE_UNITS.length - 1) {
            scaled /= 1024;
            unit++;
        }
        return format(Math.copySign(scaled, value), unit == 0 ? 0 : 1) + " " + BYTE_UNITS[unit];
    }
}
