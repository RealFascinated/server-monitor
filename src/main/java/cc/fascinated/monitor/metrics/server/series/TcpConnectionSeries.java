package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.TcpConnectionMetric;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum TcpConnectionSeries implements VmMetricFamily {
    CONNECTIONS("monitor_tcp_connections");

    private final String metricName;

    TcpConnectionSeries(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public MetricFamily metricFamily() {
        return MetricFamily.TCP;
    }

    @Override
    public String metricPrefix() {
        return "monitor_tcp_";
    }

    public static void write(MetricWriteContext ctx, TcpConnectionMetric metric) {
        CONNECTIONS.write(ctx.withLabel("state", metric.state()), metric.count());
    }
}
