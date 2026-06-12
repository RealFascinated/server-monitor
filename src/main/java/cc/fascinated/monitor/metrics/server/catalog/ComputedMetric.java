package cc.fascinated.monitor.metrics.server.catalog;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.function.LongFunction;

@Getter
@Accessors(fluent = true)
public enum ComputedMetric {
    DISK_ETA_UNTIL_FULL(MetricFamily.DISK, "etaUntilFull", ComputedMetric::diskEtaUntilFullPromql);

    private final MetricFamily family;
    private final String fieldName;
    private final LongFunction<String> promqlBuilder;

    ComputedMetric(MetricFamily family, String fieldName, LongFunction<String> promqlBuilder) {
        this.family = family;
        this.fieldName = fieldName;
        this.promqlBuilder = promqlBuilder;
    }

    public String promql(long serverId) {
        return this.promqlBuilder.apply(serverId);
    }

    private static String diskEtaUntilFullPromql(long serverId) {
        String selector = "{server_id=\"" + serverId + "\"}";
        String total = "monitor_disk_total_bytes" + selector;
        String used = "monitor_disk_used_bytes" + selector;
        return "(" + total + " - " + used + ") / clamp_min((" + used + " - " + used + " offset 1d) / 86400, 1)";
    }
}
