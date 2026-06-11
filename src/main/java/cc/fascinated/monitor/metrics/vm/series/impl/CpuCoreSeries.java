package cc.fascinated.monitor.metrics.vm.series.impl;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.vm.series.VmGaugeSeries;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.CpuCoreMetric;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum CpuCoreSeries implements VmGaugeSeries {
    USAGE_PERCENT("monitor_host_cpu_core_pct");

    private final String metricName;

    CpuCoreSeries(String metricName) {
        this.metricName = metricName;
    }

    public static void write(MetricWriteContext ctx, CpuCoreMetric core) {
        MetricWriteContext labeled = ctx.withLabel("cpu", core.cpu());
        USAGE_PERCENT.write(labeled, core.usagePercent());
    }
}
