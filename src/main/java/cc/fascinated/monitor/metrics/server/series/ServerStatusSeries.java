package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum ServerStatusSeries implements VmGaugeSeries {
    UP("monitor_server_up");

    private final String metricName;

    ServerStatusSeries(String metricName) {
        this.metricName = metricName;
    }

    public static void writeOnline(MetricWriteContext ctx) {
        UP.write(ctx, 1);
    }

    public static void writeOffline(MetricWriteContext ctx) {
        UP.write(ctx, 0);
    }
}
