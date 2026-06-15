package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.DockerContainerMetric;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Getter
@Accessors(fluent = true)
public enum DockerSeries implements VmMetricFamily {
    CPU_USAGE("monitor_container_cpu_usage"),
    MEMORY_USAGE("monitor_container_memory_usage");

    private final String metricName;

    DockerSeries(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public MetricFamily metricFamily() {
        return MetricFamily.DOCKER;
    }

    @Override
    public String metricPrefix() {
        return "monitor_container_";
    }

    public static void write(MetricWriteContext ctx, DockerContainerMetric container) {
        MetricWriteContext labeled = ctx.withLabel("container", container.containerName());
        BigDecimal cpuUsage = container.cpuUsage();
        CPU_USAGE.writeNullable(labeled, cpuUsage != null ? cpuUsage.doubleValue() : null);
        MEMORY_USAGE.writeNullable(labeled, container.memoryUsage());
    }
}
