package cc.fascinated.monitor.metrics.vm.series.impl;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.vm.series.VmGaugeSeries;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.GpuMetric;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum GpuSeries implements VmGaugeSeries {
    USAGE_PERCENT("monitor_gpu_usage_percent"),
    MEMORY_USED_BYTES("monitor_gpu_memory_used_bytes"),
    MEMORY_TOTAL_BYTES("monitor_gpu_memory_total_bytes"),
    TEMPERATURE_CELSIUS("monitor_gpu_temperature_celsius"),
    POWER_WATTS("monitor_gpu_power_watts");

    private final String metricName;

    GpuSeries(String metricName) {
        this.metricName = metricName;
    }

    public static void write(MetricWriteContext ctx, GpuMetric gpu) {
        MetricWriteContext labeled = ctx.withLabel("gpu", gpu.name()).withLabel("device_id", gpu.deviceId());
        if (gpu.vendor() != null) {
            labeled = labeled.withLabel("vendor", gpu.vendor());
        }
        USAGE_PERCENT.write(labeled, gpu.usagePercent());
        MEMORY_USED_BYTES.writeNullable(labeled, gpu.memoryUsedBytes());
        MEMORY_TOTAL_BYTES.writeNullable(labeled, gpu.memoryTotalBytes());
        TEMPERATURE_CELSIUS.writeNullable(labeled, gpu.temperatureCelsius());
        POWER_WATTS.writeNullable(labeled, gpu.powerWatts());
    }
}
