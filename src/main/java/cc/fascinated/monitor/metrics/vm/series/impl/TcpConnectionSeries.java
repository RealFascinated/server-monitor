package cc.fascinated.monitor.metrics.vm.series.impl;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.vm.series.VmGaugeSeries;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.TcpConnectionMetric;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum TcpConnectionSeries implements VmGaugeSeries {
    CONNECTIONS("monitor_tcp_connections");

    private final String metricName;

    TcpConnectionSeries(String metricName) {
        this.metricName = metricName;
    }

    public static void write(MetricWriteContext ctx, TcpConnectionMetric metric) {
        CONNECTIONS.write(ctx.withLabel("state", metric.state()), metric.count());
    }
}
