package cc.fascinated.monitor.metrics.vm.series.impl;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.vm.series.VmGaugeSeries;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.DockerContainerMetric;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum DockerSeries implements VmGaugeSeries {
    CPU_USAGE("monitor_container_cpu_usage"),
    MEMORY_USAGE("monitor_container_memory_usage");

    private final String metricName;

    DockerSeries(String metricName) {
        this.metricName = metricName;
    }

    public static void write(MetricWriteContext ctx, DockerContainerMetric container) {
        MetricWriteContext labeled = ctx.withLabel("container", container.containerName());
        CPU_USAGE.write(labeled, container.cpuUsage().doubleValue());
        MEMORY_USAGE.writeNullable(labeled, container.memoryUsage());
    }
}
