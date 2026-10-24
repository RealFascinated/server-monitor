package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.SmartMetric;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum SmartSeries implements VmMetricFamily {
    HEALTH_PASSED("monitor_smart_health_passed"),
    TEMPERATURE_CELSIUS("monitor_smart_temperature_celsius"),
    POWER_ON_HOURS("monitor_smart_power_on_hours"),
    REALLOCATED_SECTORS("monitor_smart_reallocated_sectors"),
    PENDING_SECTORS("monitor_smart_pending_sectors"),
    PERCENTAGE_USED("monitor_smart_percentage_used"),
    AVAILABLE_SPARE("monitor_smart_available_spare"),
    MEDIA_ERRORS("monitor_smart_media_errors"),
    UNSAFE_SHUTDOWNS("monitor_smart_unsafe_shutdowns"),
    DATA_WRITTEN_BYTES("monitor_smart_data_written_bytes"),
    DATA_READ_BYTES("monitor_smart_data_read_bytes");

    private final String metricName;

    SmartSeries(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public MetricFamily metricFamily() {
        return MetricFamily.SMART;
    }

    @Override
    public String metricPrefix() {
        return "monitor_smart_";
    }

    public static void write(MetricWriteContext ctx, SmartMetric disk) {
        MetricWriteContext labeled = ctx.withLabel("device", disk.device()).withLabel("model", disk.model());
        HEALTH_PASSED.writeNullable(labeled, disk.healthPassed() == null ? null : (disk.healthPassed() ? 1L : 0L));
        TEMPERATURE_CELSIUS.writeNullable(labeled, disk.temperatureCelsius());
        POWER_ON_HOURS.writeNullable(labeled, disk.powerOnHours());
        REALLOCATED_SECTORS.writeNullable(labeled, disk.reallocatedSectors());
        PENDING_SECTORS.writeNullable(labeled, disk.pendingSectors());
        PERCENTAGE_USED.writeNullable(labeled, disk.percentageUsed());
        AVAILABLE_SPARE.writeNullable(labeled, disk.availableSpare());
        MEDIA_ERRORS.writeNullable(labeled, disk.mediaErrors());
        UNSAFE_SHUTDOWNS.writeNullable(labeled, disk.unsafeShutdowns());
        DATA_WRITTEN_BYTES.writeNullable(labeled, disk.dataWrittenBytes());
        DATA_READ_BYTES.writeNullable(labeled, disk.dataReadBytes());
    }
}
