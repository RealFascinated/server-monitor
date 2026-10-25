package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.TemperatureMetric;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import org.jetbrains.annotations.Nullable;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum TemperatureSeries implements VmMetricFamily {
    CELSIUS("monitor_host_temperature_celsius",
            AlertInfo.of("Temperature", AlertUnit.TEMPERATURE, AlertOperator.MORE_THAN, 85).asDefault(300, AlertSeverity.WARNING));

    private final String metricName;
    @Nullable
    private final AlertInfo alertInfo;

    TemperatureSeries(String metricName) {
        this(metricName, null);
    }

    TemperatureSeries(String metricName, @Nullable AlertInfo alertInfo) {
        this.metricName = metricName;
        this.alertInfo = alertInfo;
    }

    @Override
    public MetricFamily metricFamily() {
        return MetricFamily.TEMPERATURE;
    }

    @Override
    public String metricPrefix() {
        return "monitor_host_";
    }

    public static void write(MetricWriteContext ctx, TemperatureMetric reading) {
        MetricWriteContext labeled = ctx.withLabel("sensor", reading.sensor());
        CELSIUS.write(labeled, reading.celsius());
    }
}
