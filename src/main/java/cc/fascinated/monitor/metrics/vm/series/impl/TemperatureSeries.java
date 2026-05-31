package cc.fascinated.monitor.metrics.vm.series.impl;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.vm.series.VmGaugeSeries;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.TemperatureMetric;

public enum TemperatureSeries implements VmGaugeSeries {
    CELSIUS("monitor_host_temperature_celsius");

    private final String metricName;

    TemperatureSeries(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String metricName() {
        return this.metricName;
    }

    public static void write(MetricWriteContext ctx, TemperatureMetric reading) {
        MetricWriteContext labeled = ctx.withLabel("sensor", reading.sensor());
        CELSIUS.write(labeled, reading.celsius());
    }
}
