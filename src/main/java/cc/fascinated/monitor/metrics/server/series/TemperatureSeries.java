package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.TemperatureMetric;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum TemperatureSeries implements VmMetricFamily {
    CELSIUS("monitor_host_temperature_celsius");

    private final String metricName;

    TemperatureSeries(String metricName) {
        this.metricName = metricName;
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
