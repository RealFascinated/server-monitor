package cc.fascinated.monitor.model.domain.metric;

import cc.fascinated.monitor.metrics.vm.query.VictoriaMetricsQuery;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

@Getter
@Accessors(fluent = true)
public enum MetricTimeRange {
    H24("24h", Duration.ofHours(24), Duration.ofMinutes(5)),
    D3("3d", Duration.ofDays(3), Duration.ofMinutes(15)),
    D7("7d", Duration.ofDays(7), Duration.ofMinutes(30)),
    W2("2w", Duration.ofDays(14), Duration.ofHours(1)),
    MO1("1mo", Duration.ofDays(30), Duration.ofHours(2)),
    MO3("3mo", Duration.ofDays(90), Duration.ofHours(6)),
    Y1("1y", Duration.ofDays(365), Duration.ofDays(1)),
    Y2("2y", Duration.ofDays(730), Duration.ofDays(2));

    private final String param;
    private final Duration lookback;
    private final Duration step;

    MetricTimeRange(String param, Duration lookback, Duration step) {
        this.param = param;
        this.lookback = lookback;
        this.step = step;
    }

    public QueryWindow queryWindow() {
        Instant to = Instant.now();
        return new QueryWindow(to.minus(this.lookback), to, this.step);
    }

    public VictoriaMetricsQuery toRangeQuery(String promql, QueryWindow window) {
        return VictoriaMetricsQuery.builder()
                .query(promql)
                .from(window.from())
                .to(window.to())
                .step(window.step())
                .build();
    }

    public static MetricTimeRange fromParam(String value) {
        return Arrays.stream(values())
                .filter(range -> range.param.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown metric range: " + value));
    }

    public record QueryWindow(Instant from, Instant to, Duration step) {
        public long stepSeconds() {
            return this.step.getSeconds();
        }
    }
}
