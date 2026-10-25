package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import org.jetbrains.annotations.Nullable;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum ServerStatusSeries implements VmGaugeSeries {
    UP("monitor_server_up",
            AlertInfo.of("Server status", AlertUnit.STATE, AlertOperator.LESS_THAN, 1).asDefault(0, AlertSeverity.CRITICAL));

    private final String metricName;
    @Nullable
    private final AlertInfo alertInfo;

    ServerStatusSeries(String metricName) {
        this(metricName, null);
    }

    ServerStatusSeries(String metricName, @Nullable AlertInfo alertInfo) {
        this.metricName = metricName;
        this.alertInfo = alertInfo;
    }

    public static void writeOnline(MetricWriteContext ctx) {
        UP.write(ctx, 1);
    }

    public static void writeOffline(MetricWriteContext ctx) {
        UP.write(ctx, 0);
    }
}
