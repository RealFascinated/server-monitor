package cc.fascinated.monitor.model.domain.metric;

import cc.fascinated.monitor.model.domain.settings.Preference;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MetricRefreshInterval implements Preference {
    TEN_SECONDS("10s"),
    THIRTY_SECONDS("30s"),
    ONE_MINUTE("1m"),
    NEVER("never");

    private final String key;
}
