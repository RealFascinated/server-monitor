package cc.fascinated.monitor.metrics.platform.collector;

import cc.fascinated.monitor.metrics.platform.catalog.PlatformMetricFamily;
import cc.fascinated.monitor.metrics.vm.VictoriaMetricsSelfMetricsClient;
import cc.fascinated.monitor.metrics.vm.write.PrometheusWriteContext;
import org.springframework.stereotype.Component;

@Component
public class VmStorageMetricsCollector {
    private final VictoriaMetricsSelfMetricsClient victoriaMetricsSelfMetricsClient;

    public VmStorageMetricsCollector(VictoriaMetricsSelfMetricsClient victoriaMetricsSelfMetricsClient) {
        this.victoriaMetricsSelfMetricsClient = victoriaMetricsSelfMetricsClient;
    }

    public void write(PrometheusWriteContext ctx) {
        VictoriaMetricsSelfMetricsClient.SelfMetrics metrics = this.victoriaMetricsSelfMetricsClient.read();
        metrics.storageSizeBytes().ifPresent(size ->
                ctx.gauge(PlatformMetricFamily.VM_STORAGE_SIZE_BYTES.metricName(), size)
        );
        metrics.datapointCount().ifPresent(count ->
                ctx.gauge(PlatformMetricFamily.VM_DATAPOINT_COUNT.metricName(), count)
        );
    }
}
