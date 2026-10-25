package cc.fascinated.monitor.util;

import lombok.experimental.UtilityClass;

import java.time.Duration;

@UtilityClass
public class DurationUtils {

    /** Compact human form such as {@code 45s}, {@code 1h 30m} or {@code 2d 3h}. */
    public static String format(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return minutes % 60 == 0 ? hours + "h" : hours + "h " + (minutes % 60) + "m";
        }
        return hours % 24 == 0 ? hours / 24 + "d" : hours / 24 + "d " + (hours % 24) + "h";
    }
}
