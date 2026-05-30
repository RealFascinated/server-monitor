package cc.fascinated.monitor.metrics.gauge;

import cc.fascinated.monitor.metrics.holder.GaugeHolder;
import cc.fascinated.monitor.metrics.support.RefreshableMetric;
import cc.fascinated.monitor.service.PlatformMetricsQueryService;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.stereotype.Component;

@Component
public class StaleServersGaugeMetric implements RefreshableMetric {
    private final GaugeHolder gauge;
    private final PlatformMetricsQueryService queries;

    public StaleServersGaugeMetric(PrometheusRegistry registry, PlatformMetricsQueryService queries) {
        this.gauge = GaugeHolder.register(registry, "monitor_servers_stale", "Servers not reporting within the configured window");
        this.queries = queries;
    }

    @Override
    public void refresh() {
        this.gauge.set(this.queries.fleetCounts().stale());
    }
}
