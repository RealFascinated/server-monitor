package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ZfsPoolMetric;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import org.jetbrains.annotations.Nullable;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum ZfsPoolSeries implements VmMetricFamily {
    CAPACITY_PERCENT("monitor_zfs_pool_capacity_percent",
            AlertInfo.of("ZFS pool capacity", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 85)),
    ALLOCATED_BYTES("monitor_zfs_pool_allocated_bytes"),
    FREE_BYTES("monitor_zfs_pool_free_bytes"),
    TOTAL_BYTES("monitor_zfs_pool_total_bytes"),
    FRAGMENTATION_PERCENT("monitor_zfs_pool_fragmentation_percent",
            AlertInfo.of("ZFS pool fragmentation", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 80)),
    SCAN_PERCENT("monitor_zfs_pool_scan_percent"),
    READ_BPS("monitor_zfs_pool_read_bps"),
    WRITE_BPS("monitor_zfs_pool_write_bps"),
    READ_IOPS("monitor_zfs_pool_read_iops"),
    WRITE_IOPS("monitor_zfs_pool_write_iops"),
    CHECKSUM_ERRORS("monitor_zfs_pool_checksum_errors",
            AlertInfo.of("ZFS checksum errors", AlertUnit.COUNT, AlertOperator.MORE_THAN, 0));

    private final String metricName;
    @Nullable
    private final AlertInfo alertInfo;

    ZfsPoolSeries(String metricName) {
        this(metricName, null);
    }

    ZfsPoolSeries(String metricName, @Nullable AlertInfo alertInfo) {
        this.metricName = metricName;
        this.alertInfo = alertInfo;
    }

    @Override
    public MetricFamily metricFamily() {
        return MetricFamily.ZFS_POOL;
    }

    @Override
    public String metricPrefix() {
        return "monitor_zfs_pool_";
    }

    public static void write(MetricWriteContext ctx, ZfsPoolMetric pool) {
        MetricWriteContext labeled = ctx.withLabel("pool", pool.poolName())
                .withLabel("health", pool.health())
                .withLabel("scan_state", pool.scanState());
        CAPACITY_PERCENT.write(labeled, pool.capacityPercent());
        ALLOCATED_BYTES.write(labeled, pool.allocatedBytes());
        FREE_BYTES.write(labeled, pool.freeBytes());
        TOTAL_BYTES.write(labeled, pool.totalBytes());
        FRAGMENTATION_PERCENT.write(labeled, pool.fragmentationPercent());
        SCAN_PERCENT.write(labeled, pool.scanPercent());
        READ_BPS.write(labeled, pool.readBps());
        WRITE_BPS.write(labeled, pool.writeBps());
        READ_IOPS.write(labeled, pool.readIops());
        WRITE_IOPS.write(labeled, pool.writeIops());
        CHECKSUM_ERRORS.write(labeled, pool.checksumErrors());
    }
}
