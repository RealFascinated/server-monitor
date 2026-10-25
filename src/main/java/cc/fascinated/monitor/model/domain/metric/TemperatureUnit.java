package cc.fascinated.monitor.model.domain.metric;

import cc.fascinated.monitor.model.domain.settings.Preference;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/** How a user wants temperatures shown. Stored metrics and alert thresholds are always in Celsius. */
@RequiredArgsConstructor
@Getter
public enum TemperatureUnit implements Preference {
    CELSIUS("celsius", "°C", 1, 0),
    FAHRENHEIT("fahrenheit", "°F", 1.8, 32);

    private final String key;
    @Accessors(fluent = true)
    private final String symbol;
    private final double scale;
    private final double offset;

    public static TemperatureUnit fromKey(String key) {
        for (TemperatureUnit unit : values()) {
            if (unit.key.equals(key)) {
                return unit;
            }
        }
        return CELSIUS;
    }

    public double fromCelsius(double celsius) {
        return celsius * this.scale + this.offset;
    }
}
