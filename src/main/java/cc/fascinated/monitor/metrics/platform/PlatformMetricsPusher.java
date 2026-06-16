package cc.fascinated.monitor.metrics.platform;

import cc.fascinated.monitor.metrics.platform.collector.FleetMetricsCollector;
import cc.fascinated.monitor.metrics.platform.collector.JvmMetricsCollector;
import cc.fascinated.monitor.metrics.platform.collector.PlatformMetricsRecorder;
import cc.fascinated.monitor.metrics.platform.collector.VmStorageMetricsCollector;
import cc.fascinated.monitor.metrics.vm.write.PrometheusWriteContext;
import cc.fascinated.monitor.metrics.vm.VictoriaMetricsWriteClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class PlatformMetricsPusher {
    private final FleetMetricsCollector fleetMetricsCollector;
    private final JvmMetricsCollector jvmMetricsCollector;
    private final VmStorageMetricsCollector vmStorageMetricsCollector;
    private final PlatformMetricsRecorder platformMetricsRecorder;
    private final VictoriaMetricsWriteClient victoriaMetricsWriteClient;

    public PlatformMetricsPusher(FleetMetricsCollector fleetMetricsCollector, JvmMetricsCollector jvmMetricsCollector,
                                 VmStorageMetricsCollector vmStorageMetricsCollector,
                                 PlatformMetricsRecorder platformMetricsRecorder,
                                 VictoriaMetricsWriteClient victoriaMetricsWriteClient) {
        this.fleetMetricsCollector = fleetMetricsCollector;
        this.jvmMetricsCollector = jvmMetricsCollector;
        this.vmStorageMetricsCollector = vmStorageMetricsCollector;
        this.platformMetricsRecorder = platformMetricsRecorder;
        this.victoriaMetricsWriteClient = victoriaMetricsWriteClient;
    }

    public void push() {
        long epochSeconds = Instant.now().getEpochSecond();
        StringBuilder buffer = new StringBuilder();
        PrometheusWriteContext ctx = new PrometheusWriteContext(buffer, epochSeconds);
        this.fleetMetricsCollector.write(ctx);
        this.jvmMetricsCollector.write(ctx);
        this.vmStorageMetricsCollector.write(ctx);
        this.platformMetricsRecorder.writeCountersAndHistograms(ctx);
        if (buffer.isEmpty()) {
            return;
        }
        try {
            this.victoriaMetricsWriteClient.flush(buffer.toString());
        } catch (Exception ex) {
            log.warn("Failed to push platform metrics to VictoriaMetrics: {}", ex.getMessage());
        }
    }
}
