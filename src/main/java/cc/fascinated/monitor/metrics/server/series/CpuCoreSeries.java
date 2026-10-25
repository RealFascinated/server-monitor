package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.CpuCoreMetric;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

@Getter
@Accessors(fluent = true)
public enum CpuCoreSeries implements VmMetricFamily {
    USAGE_PERCENT("monitor_host_cpu_core_pct",
            AlertInfo.of("CPU core usage", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 95));

    private final String metricName;
    @Nullable
    private final AlertInfo alertInfo;

    CpuCoreSeries(String metricName) {
        this(metricName, null);
    }

    CpuCoreSeries(String metricName, @Nullable AlertInfo alertInfo) {
        this.metricName = metricName;
        this.alertInfo = alertInfo;
    }

    @Override
    public MetricFamily metricFamily() {
        return MetricFamily.CPU_CORE;
    }

    @Override
    public String metricPrefix() {
        return "monitor_host_";
    }

    public static void write(MetricWriteContext ctx, CpuCoreMetric core) {
        MetricWriteContext labeled = ctx.withLabel("cpu", core.cpu());
        USAGE_PERCENT.write(labeled, core.usagePercent());
    }
}
