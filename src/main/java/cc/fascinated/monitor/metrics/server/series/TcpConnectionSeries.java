package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.TcpConnectionMetric;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import org.jetbrains.annotations.Nullable;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum TcpConnectionSeries implements VmMetricFamily {
    CONNECTIONS("monitor_tcp_connections",
            AlertInfo.of("TCP connections", AlertUnit.COUNT, AlertOperator.MORE_THAN, 10000));

    private final String metricName;
    @Nullable
    private final AlertInfo alertInfo;

    TcpConnectionSeries(String metricName, @Nullable AlertInfo alertInfo) {
        this.metricName = metricName;
        this.alertInfo = alertInfo;
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
