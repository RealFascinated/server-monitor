package cc.fascinated.monitor.metrics.vm.series.impl;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.vm.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.vm.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.CpuCoreMetric;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum CpuCoreSeries implements VmMetricFamily {
    USAGE_PERCENT("monitor_host_cpu_core_pct");

    private final String metricName;

    CpuCoreSeries(String metricName) {
        this.metricName = metricName;
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
