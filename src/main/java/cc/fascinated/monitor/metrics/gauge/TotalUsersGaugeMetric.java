package cc.fascinated.monitor.metrics.gauge;

import cc.fascinated.monitor.metrics.holder.GaugeHolder;
import cc.fascinated.monitor.metrics.support.RefreshableMetric;
import cc.fascinated.monitor.service.PlatformMetricsQueryService;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.stereotype.Component;

@Component
public class TotalUsersGaugeMetric implements RefreshableMetric {
    private final GaugeHolder gauge;
    private final PlatformMetricsQueryService queries;

    public TotalUsersGaugeMetric(PrometheusRegistry registry, PlatformMetricsQueryService queries) {
        this.gauge = GaugeHolder.register(registry, "monitor_users", "Total registered users");
        this.queries = queries;
    }

    @Override
    public void refresh() {
        this.gauge.set(this.queries.countUsers());
    }
}
