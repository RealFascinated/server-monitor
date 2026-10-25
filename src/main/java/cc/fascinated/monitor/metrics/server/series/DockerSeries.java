package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.DockerContainerMetric;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import org.jetbrains.annotations.Nullable;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Getter
@Accessors(fluent = true)
public enum DockerSeries implements VmMetricFamily {
    CPU_USAGE("monitor_container_cpu_usage",
            AlertInfo.of("Container CPU usage", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 90)),
    MEMORY_USAGE("monitor_container_memory_usage",
            AlertInfo.of("Container memory usage", AlertUnit.BYTES, AlertOperator.MORE_THAN, 1073741824)),
    NETWORK_RX_BYTES_PER_SECOND("monitor_container_network_rx_bytes_per_second"),
    NETWORK_TX_BYTES_PER_SECOND("monitor_container_network_tx_bytes_per_second"),
    BLOCK_READ_BYTES_PER_SECOND("monitor_container_block_read_bytes_per_second"),
    BLOCK_WRITE_BYTES_PER_SECOND("monitor_container_block_write_bytes_per_second");

    private final String metricName;
    @Nullable
    private final AlertInfo alertInfo;

    DockerSeries(String metricName) {
        this(metricName, null);
    }

    DockerSeries(String metricName, @Nullable AlertInfo alertInfo) {
        this.metricName = metricName;
        this.alertInfo = alertInfo;
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
        NETWORK_RX_BYTES_PER_SECOND.writeNullable(labeled, container.networkRxBytesPerSecond());
        NETWORK_TX_BYTES_PER_SECOND.writeNullable(labeled, container.networkTxBytesPerSecond());
        BLOCK_READ_BYTES_PER_SECOND.writeNullable(labeled, container.blockReadBytesPerSecond());
        BLOCK_WRITE_BYTES_PER_SECOND.writeNullable(labeled, container.blockWriteBytesPerSecond());
    }
}
