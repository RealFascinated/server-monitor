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
    POWER_CYCLE_COUNT("monitor_smart_power_cycle_count"),
    REALLOCATED_SECTORS("monitor_smart_reallocated_sectors"),
    REALLOCATED_EVENTS("monitor_smart_reallocated_events"),
    PENDING_SECTORS("monitor_smart_pending_sectors"),
    REPORTED_UNCORRECT("monitor_smart_reported_uncorrect"),
    OFFLINE_UNCORRECTABLE("monitor_smart_offline_uncorrectable"),
    UDMA_CRC_ERRORS("monitor_smart_udma_crc_errors"),
    LOAD_CYCLE_COUNT("monitor_smart_load_cycle_count"),
    START_STOP_COUNTS("monitor_smart_start_stop_counts"),
    POWER_OFF_RETRACTS("monitor_smart_power_off_retracts"),
    HELIUM_PERCENT("monitor_smart_helium_percent"),
    ERROR_LOG_ENTRIES("monitor_smart_error_log_entries"),
    ENDURANCE_USED_PERCENT("monitor_smart_endurance_used_percent"),
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
        POWER_CYCLE_COUNT.writeNullable(labeled, disk.powerCycleCount());
        REALLOCATED_SECTORS.writeNullable(labeled, disk.reallocatedSectors());
        REALLOCATED_EVENTS.writeNullable(labeled, disk.reallocatedEvents());
        PENDING_SECTORS.writeNullable(labeled, disk.pendingSectors());
        REPORTED_UNCORRECT.writeNullable(labeled, disk.reportedUncorrect());
        OFFLINE_UNCORRECTABLE.writeNullable(labeled, disk.offlineUncorrectable());
        UDMA_CRC_ERRORS.writeNullable(labeled, disk.udmaCrcErrors());
        LOAD_CYCLE_COUNT.writeNullable(labeled, disk.loadCycleCount());
        START_STOP_COUNTS.writeNullable(labeled, disk.startStopCounts());
        POWER_OFF_RETRACTS.writeNullable(labeled, disk.powerOffRetracts());
        HELIUM_PERCENT.writeNullable(labeled, disk.heliumPercent());
        ERROR_LOG_ENTRIES.writeNullable(labeled, disk.errorLogEntries());
        ENDURANCE_USED_PERCENT.writeNullable(labeled, disk.enduranceUsedPercent());
        PERCENTAGE_USED.writeNullable(labeled, disk.percentageUsed());
        AVAILABLE_SPARE.writeNullable(labeled, disk.availableSpare());
        MEDIA_ERRORS.writeNullable(labeled, disk.mediaErrors());
        UNSAFE_SHUTDOWNS.writeNullable(labeled, disk.unsafeShutdowns());
        DATA_WRITTEN_BYTES.writeNullable(labeled, disk.dataWrittenBytes());
        DATA_READ_BYTES.writeNullable(labeled, disk.dataReadBytes());
    }
}
