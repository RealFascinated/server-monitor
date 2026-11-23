package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.MdArrayMetric;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import org.jetbrains.annotations.Nullable;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum MdArraySeries implements VmMetricFamily {
    DEGRADED("monitor_md_array_degraded",
            AlertInfo.of("RAID array degraded", AlertUnit.COUNT, AlertOperator.MORE_THAN, 0)),
    DISKS_TOTAL("monitor_md_array_disks_total"),
    DISKS_ACTIVE("monitor_md_array_disks_active"),
    DISKS_FAILED("monitor_md_array_disks_failed",
            AlertInfo.of("RAID array failed disks", AlertUnit.COUNT, AlertOperator.MORE_THAN, 0)),
    SYNC_PERCENT("monitor_md_array_sync_percent"),
    SYNC_SPEED_KBPS("monitor_md_array_sync_speed_kbps"),
    SYNC_FINISH_MINUTES("monitor_md_array_sync_finish_minutes");

    private final String metricName;
    @Nullable
    private final AlertInfo alertInfo;

    MdArraySeries(String metricName) {
        this(metricName, null);
    }

    MdArraySeries(String metricName, @Nullable AlertInfo alertInfo) {
        this.metricName = metricName;
        this.alertInfo = alertInfo;
    }

    @Override
    public MetricFamily metricFamily() {
        return MetricFamily.MD_ARRAY;
    }

    @Override
    public String metricPrefix() {
        return "monitor_md_array_";
    }

    public static void write(MetricWriteContext ctx, MdArrayMetric array) {
        MetricWriteContext labeled = ctx.withLabel("array", array.name())
                .withLabel("level", array.level());
        if (array.syncAction() != null) {
            labeled = labeled.withLabel("sync_action", array.syncAction());
        }
        DEGRADED.writeNullable(labeled, array.degraded() == null ? null : (array.degraded() ? 1L : 0L));
        DISKS_TOTAL.writeNullable(labeled, array.disksTotal());
        DISKS_ACTIVE.writeNullable(labeled, array.disksActive());
        DISKS_FAILED.writeNullable(labeled, array.disksFailed());
        SYNC_PERCENT.writeNullable(labeled, array.syncPercent());
        SYNC_SPEED_KBPS.writeNullable(labeled, array.syncSpeedKBps());
        SYNC_FINISH_MINUTES.writeNullable(labeled, array.syncFinishMinutes());
    }
}
