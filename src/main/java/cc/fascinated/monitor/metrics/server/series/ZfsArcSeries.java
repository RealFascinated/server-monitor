package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ZfsArcMetrics;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum ZfsArcSeries implements VmMetricFamily {
    ARC_SIZE_BYTES("monitor_zfs_arc_size_bytes"),
    ARC_TARGET_BYTES("monitor_zfs_arc_target_bytes"),
    ARC_MAX_BYTES("monitor_zfs_arc_max_bytes"),
    ARC_MIN_BYTES("monitor_zfs_arc_min_bytes"),
    ARC_DATA_BYTES("monitor_zfs_arc_data_bytes"),
    ARC_METADATA_BYTES("monitor_zfs_arc_metadata_bytes"),
    L2ARC_SIZE_BYTES("monitor_zfs_arc_l2arc_size_bytes"),
    ARC_HIT_RATIO("monitor_zfs_arc_hit_ratio"),
    ARC_MISSES_PER_SECOND("monitor_zfs_arc_misses_per_second");

    private final String metricName;

    ZfsArcSeries(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public MetricFamily metricFamily() {
        return MetricFamily.ZFS_ARC;
    }

    @Override
    public String metricPrefix() {
        return "monitor_zfs_arc_";
    }

    public static void write(MetricWriteContext ctx, ZfsArcMetrics arc) {
        ARC_SIZE_BYTES.write(ctx, arc.arcSizeBytes());
        ARC_TARGET_BYTES.write(ctx, arc.arcTargetBytes());
        ARC_MAX_BYTES.write(ctx, arc.arcMaxBytes());
        ARC_MIN_BYTES.write(ctx, arc.arcMinBytes());
        ARC_DATA_BYTES.write(ctx, arc.arcDataBytes());
        ARC_METADATA_BYTES.write(ctx, arc.arcMetadataBytes());
        L2ARC_SIZE_BYTES.write(ctx, arc.l2arcSizeBytes());
        ARC_HIT_RATIO.write(ctx, arc.arcHitRatio());
        ARC_MISSES_PER_SECOND.write(ctx, arc.arcMissesPerSecond());
    }
}
