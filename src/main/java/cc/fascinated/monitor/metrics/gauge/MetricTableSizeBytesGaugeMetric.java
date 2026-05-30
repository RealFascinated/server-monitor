package cc.fascinated.monitor.metrics.gauge;

import cc.fascinated.monitor.metrics.holder.GaugeHolder;
import cc.fascinated.monitor.metrics.support.RefreshableMetric;
import cc.fascinated.monitor.service.PlatformMetricsQueryService;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.stereotype.Component;

@Component
public class MetricTableSizeBytesGaugeMetric implements RefreshableMetric {
    private final GaugeHolder gauge;
    private final PlatformMetricsQueryService queries;

    public MetricTableSizeBytesGaugeMetric(PrometheusRegistry registry, PlatformMetricsQueryService queries) {
        this.gauge = GaugeHolder.register(registry, "monitor_table_size_bytes", "On-disk size per metrics table in bytes", "table");
        this.queries = queries;
    }

    @Override
    public void refresh() {
        for (String table : this.queries.metricTables()) {
            this.gauge.set(this.queries.tableSizeBytes(table), table);
        }
    }
}
