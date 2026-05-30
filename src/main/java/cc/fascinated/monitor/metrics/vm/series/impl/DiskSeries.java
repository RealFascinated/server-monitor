package cc.fascinated.monitor.metrics.vm.series.impl;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.vm.series.VmGaugeSeries;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.DiskMetric;

public enum DiskSeries implements VmGaugeSeries {
    USAGE_PERCENT("monitor_disk_usage_percent"),
    USED_BYTES("monitor_disk_used_bytes"),
    TOTAL_BYTES("monitor_disk_total_bytes"),
    IO_READ_BPS("monitor_disk_io_read_bps"),
    IO_WRITE_BPS("monitor_disk_io_write_bps"),
    IO_USAGE_PCT("monitor_disk_io_usage_pct"),
    IO_WAIT_MS("monitor_disk_io_wait_ms"),
    INODE_USED("monitor_disk_inode_used"),
    INODE_TOTAL("monitor_disk_inode_total"),
    READ_IOPS("monitor_disk_read_iops"),
    WRITE_IOPS("monitor_disk_write_iops"),
    READ_LATENCY_MS("monitor_disk_read_latency_ms"),
    WRITE_LATENCY_MS("monitor_disk_write_latency_ms");

    private final String metricName;

    DiskSeries(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String metricName() {
        return this.metricName;
    }

    public static void write(MetricWriteContext ctx, DiskMetric disk) {
        MetricWriteContext labeled = ctx.withLabel("disk", disk.diskName());
        USED_BYTES.writeNullable(labeled, disk.usedBytes());
        TOTAL_BYTES.writeNullable(labeled, disk.totalBytes());
        Long used = disk.usedBytes();
        Long total = disk.totalBytes();
        if (used != null && total != null && total > 0) {
            USAGE_PERCENT.write(labeled, used * 100.0 / total);
        }
        IO_READ_BPS.writeNullable(labeled, disk.ioReadBytesPerSecond());
        IO_WRITE_BPS.writeNullable(labeled, disk.ioWriteBytesPerSecond());
        IO_USAGE_PCT.writeNullable(labeled, disk.ioUsagePercent());
        IO_WAIT_MS.writeNullable(labeled, disk.ioWaitMilliseconds());
        INODE_USED.writeNullable(labeled, disk.inodeUsed());
        INODE_TOTAL.writeNullable(labeled, disk.inodeTotal());
        READ_IOPS.writeNullable(labeled, disk.readIops());
        WRITE_IOPS.writeNullable(labeled, disk.writeIops());
        READ_LATENCY_MS.writeNullable(labeled, disk.readLatencyMs());
        WRITE_LATENCY_MS.writeNullable(labeled, disk.writeLatencyMs());
    }
}
