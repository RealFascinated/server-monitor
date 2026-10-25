package cc.fascinated.monitor.model.domain.alert;

import cc.fascinated.monitor.model.domain.metric.TemperatureUnit;
import cc.fascinated.monitor.util.NumberUtils;

public enum AlertUnit {
    PERCENT,
    BYTES,
    BYTES_PER_SECOND,
    MILLISECONDS,
    TEMPERATURE,
    WATTS,
    COUNT,
    /** A 1 = healthy, 0 = unhealthy gauge, such as server up or drive health. */
    STATE,
    NONE;

    public String format(double value) {
        return format(value, TemperatureUnit.CELSIUS);
    }

    /** Formats a value in its stored unit; temperatures are shown in the reader's preferred unit. */
    public String format(double value, TemperatureUnit temperatureUnit) {
        return switch (this) {
            case PERCENT -> NumberUtils.format(value, 2) + "%";
            case BYTES -> NumberUtils.formatBytes(value);
            case BYTES_PER_SECOND -> NumberUtils.formatBytes(value) + "/s";
            case MILLISECONDS -> NumberUtils.format(value, 1) + " ms";
            case TEMPERATURE -> NumberUtils.format(temperatureUnit.fromCelsius(value), 1) + temperatureUnit.symbol();
            case WATTS -> NumberUtils.format(value, 1) + " W";
            case STATE -> value >= 0.5 ? "healthy" : "unhealthy";
            case COUNT, NONE -> NumberUtils.format(value, 2);
        };
    }
}
