package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.GpuMetric;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import org.jetbrains.annotations.Nullable;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum GpuSeries implements VmMetricFamily {
    USAGE_PERCENT("monitor_gpu_usage_percent",
            AlertInfo.of("GPU usage", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 95)),
    ENCODER_USAGE_PERCENT("monitor_gpu_encoder_usage_percent",
            AlertInfo.of("GPU encoder usage", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 95)),
    DECODER_USAGE_PERCENT("monitor_gpu_decoder_usage_percent",
            AlertInfo.of("GPU decoder usage", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 95)),
    MEMORY_USED_BYTES("monitor_gpu_memory_used_bytes"),
    MEMORY_TOTAL_BYTES("monitor_gpu_memory_total_bytes"),
    TEMPERATURE_CELSIUS("monitor_gpu_temperature_celsius",
            AlertInfo.of("GPU temperature", AlertUnit.TEMPERATURE, AlertOperator.MORE_THAN, 85)),
    POWER_WATTS("monitor_gpu_power_watts",
            AlertInfo.of("GPU power", AlertUnit.WATTS, AlertOperator.MORE_THAN, 300));

    private final String metricName;
    @Nullable
    private final AlertInfo alertInfo;

    GpuSeries(String metricName) {
        this(metricName, null);
    }

    GpuSeries(String metricName, @Nullable AlertInfo alertInfo) {
        this.metricName = metricName;
        this.alertInfo = alertInfo;
    }

    @Override
    public MetricFamily metricFamily() {
        return MetricFamily.GPU;
    }

    @Override
    public String metricPrefix() {
        return "monitor_gpu_";
    }

    public static void write(MetricWriteContext ctx, GpuMetric gpu) {
        MetricWriteContext labeled = ctx.withLabel("gpu", gpu.name()).withLabel("device_id", gpu.deviceId());
        if (gpu.vendor() != null) {
            labeled = labeled.withLabel("vendor", gpu.vendor());
        }
        USAGE_PERCENT.write(labeled, gpu.usagePercent());
        ENCODER_USAGE_PERCENT.writeNullable(labeled, gpu.encoderUsagePercent());
        DECODER_USAGE_PERCENT.writeNullable(labeled, gpu.decoderUsagePercent());
        MEMORY_USED_BYTES.writeNullable(labeled, gpu.memoryUsedBytes());
        MEMORY_TOTAL_BYTES.writeNullable(labeled, gpu.memoryTotalBytes());
        TEMPERATURE_CELSIUS.writeNullable(labeled, gpu.temperatureCelsius());
        POWER_WATTS.writeNullable(labeled, gpu.powerWatts());
    }
}
