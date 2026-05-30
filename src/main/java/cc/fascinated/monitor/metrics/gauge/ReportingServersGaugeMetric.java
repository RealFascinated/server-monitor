package cc.fascinated.monitor.metrics.gauge;

import cc.fascinated.monitor.metrics.config.MonitorMetricsProperties;
import cc.fascinated.monitor.metrics.holder.GaugeHolder;
import cc.fascinated.monitor.metrics.support.RefreshableMetric;
import cc.fascinated.monitor.service.PlatformMetricsQueryService;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.stereotype.Component;

@Component
public class ReportingServersGaugeMetric implements RefreshableMetric {
    private final GaugeHolder gauge;
    private final PlatformMetricsQueryService queries;
    private final String windowLabel;

    public ReportingServersGaugeMetric(PrometheusRegistry registry, PlatformMetricsQueryService queries,
                                       MonitorMetricsProperties metricsProperties) {
        this.gauge = GaugeHolder.register(registry, "monitor_servers_reporting", "Servers reporting within the configured window", "window");
        this.queries = queries;
        this.windowLabel = metricsProperties.getReportingWindowLabel();
    }

    @Override
    public void refresh() {
        this.gauge.set(this.queries.fleetCounts().reporting(), this.windowLabel);
    }
}
