package cc.fascinated.monitor.model.domain.metric;

import cc.fascinated.monitor.model.domain.settings.Preference;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MetricTimeRange implements Preference {
    ONE_HOUR("1h"),
    THREE_HOURS("3h"),
    SIX_HOURS("6h"),
    TWELVE_HOURS("12h"),
    TWENTY_FOUR_HOURS("24h"),
    THREE_DAYS("3d"),
    SEVEN_DAYS("7d"),
    TWO_WEEKS("2w"),
    ONE_MONTH("1mo"),
    THREE_MONTHS("3mo"),
    ONE_YEAR("1y"),
    TWO_YEARS("2y");

    private final String key;
}
