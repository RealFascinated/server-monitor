package cc.fascinated.monitor.metrics.platform.collector;

import cc.fascinated.monitor.metrics.platform.catalog.PlatformMetricFamily;
import cc.fascinated.monitor.metrics.vm.write.PrometheusWriteContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FleetMetricsCollector {
    private final PlatformMetricsQueryService queries;

    public FleetMetricsCollector(PlatformMetricsQueryService queries) {
        this.queries = queries;
    }

    public void write(PrometheusWriteContext ctx) {
        ctx.gauge(PlatformMetricFamily.USERS.metricName(), this.queries.countUsers());
        ctx.gauge(PlatformMetricFamily.USERS_NEW_24H.metricName(), this.queries.countNewUsers24h());
        ctx.gauge(PlatformMetricFamily.SERVERS_NEW_24H.metricName(), this.queries.countNewServers24h());
        ctx.gauge(PlatformMetricFamily.DATABASE_SIZE_BYTES.metricName(), this.queries.databaseSizeBytes());
        ctx.gauge(PlatformMetricFamily.ACTIVE_SESSIONS.metricName(), this.queries.countActiveSessions());

        PlatformMetricsQueryService.ServerStatusCounts statusCounts = this.queries.serverStatusCounts();
        ctx.gauge(PlatformMetricFamily.SERVERS_TOTAL.metricName(), statusCounts.total());
        ctx.gauge(PlatformMetricFamily.SERVERS_ONLINE.metricName(), statusCounts.online());
        ctx.gauge(PlatformMetricFamily.SERVERS_OFFLINE.metricName(), statusCounts.offline());
        ctx.gauge(PlatformMetricFamily.SERVERS_PENDING.metricName(), statusCounts.pending());

        for (Map.Entry<String, Long> entry : this.queries.serversByAgentVersion().entrySet()) {
            ctx.gauge(PlatformMetricFamily.SERVERS_BY_AGENT_VERSION.metricName(), entry.getValue(), Map.of("version", entry.getKey()));
        }
        for (Map.Entry<String, Long> entry : this.queries.serversByOs().entrySet()) {
            ctx.gauge(PlatformMetricFamily.SERVERS_BY_OS.metricName(), entry.getValue(), Map.of("os", entry.getKey()));
        }
    }
}
