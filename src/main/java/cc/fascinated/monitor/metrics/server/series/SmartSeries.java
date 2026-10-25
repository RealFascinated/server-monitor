package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.SmartMetric;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import org.jetbrains.annotations.Nullable;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum SmartSeries implements VmMetricFamily {
    HEALTH_PASSED("monitor_smart_health_passed",
            AlertInfo.of("Drive health", AlertUnit.STATE, AlertOperator.LESS_THAN, 1).asDefault(0, AlertSeverity.CRITICAL)),
    TEMPERATURE_CELSIUS("monitor_smart_temperature_celsius",
            AlertInfo.of("Drive temperature", AlertUnit.TEMPERATURE, AlertOperator.MORE_THAN, 60).asDefault(300, AlertSeverity.WARNING)),
    POWER_ON_HOURS("monitor_smart_power_on_hours"),
    POWER_CYCLE_COUNT("monitor_smart_power_cycle_count"),
    REALLOCATED_SECTORS("monitor_smart_reallocated_sectors",
            AlertInfo.of("Reallocated sectors", AlertUnit.COUNT, AlertOperator.MORE_THAN, 0).asDefault(0, AlertSeverity.WARNING)),
    REALLOCATED_EVENTS("monitor_smart_reallocated_events"),
    PENDING_SECTORS("monitor_smart_pending_sectors",
            AlertInfo.of("Pending sectors", AlertUnit.COUNT, AlertOperator.MORE_THAN, 0).asDefault(0, AlertSeverity.WARNING)),
    REPORTED_UNCORRECT("monitor_smart_reported_uncorrect",
            AlertInfo.of("Reported uncorrectable errors", AlertUnit.COUNT, AlertOperator.MORE_THAN, 0).asDefault(0, AlertSeverity.WARNING)),
    OFFLINE_UNCORRECTABLE("monitor_smart_offline_uncorrectable",
            AlertInfo.of("Offline uncorrectable sectors", AlertUnit.COUNT, AlertOperator.MORE_THAN, 0).asDefault(0, AlertSeverity.WARNING)),
    UDMA_CRC_ERRORS("monitor_smart_udma_crc_errors",
            AlertInfo.of("UDMA CRC errors", AlertUnit.COUNT, AlertOperator.MORE_THAN, 0).asDefault(0, AlertSeverity.WARNING)),
    LOAD_CYCLE_COUNT("monitor_smart_load_cycle_count"),
    START_STOP_COUNTS("monitor_smart_start_stop_counts"),
    POWER_OFF_RETRACTS("monitor_smart_power_off_retracts"),
    HELIUM_PERCENT("monitor_smart_helium_percent"),
    ERROR_LOG_ENTRIES("monitor_smart_error_log_entries",
            AlertInfo.of("Drive error log entries", AlertUnit.COUNT, AlertOperator.MORE_THAN, 0)),
    ENDURANCE_USED_PERCENT("monitor_smart_endurance_used_percent",
            AlertInfo.of("Drive endurance used", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 90).asDefault(0, AlertSeverity.WARNING)),
    PERCENTAGE_USED("monitor_smart_percentage_used",
            AlertInfo.of("NVMe life used", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 90).asDefault(0, AlertSeverity.WARNING)),
    AVAILABLE_SPARE("monitor_smart_available_spare",
            AlertInfo.of("NVMe available spare", AlertUnit.PERCENT, AlertOperator.LESS_THAN, 10).asDefault(0, AlertSeverity.WARNING)),
    MEDIA_ERRORS("monitor_smart_media_errors",
            AlertInfo.of("NVMe media errors", AlertUnit.COUNT, AlertOperator.MORE_THAN, 0).asDefault(0, AlertSeverity.WARNING)),
    UNSAFE_SHUTDOWNS("monitor_smart_unsafe_shutdowns"),
    DATA_WRITTEN_BYTES("monitor_smart_data_written_bytes"),
    DATA_READ_BYTES("monitor_smart_data_read_bytes");

    private final String metricName;
    @Nullable
    private final AlertInfo alertInfo;

    SmartSeries(String metricName) {
        this(metricName, null);
    }

    SmartSeries(String metricName, @Nullable AlertInfo alertInfo) {
        this.metricName = metricName;
        this.alertInfo = alertInfo;
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
