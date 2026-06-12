package cc.fascinated.monitor.metrics.vm.write;

import cc.fascinated.monitor.metrics.shared.support.HistogramAccumulator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PrometheusWriteContextTest {
    @Test
    void writesGaugeCounterAndHistogramLines() {
        StringBuilder buffer = new StringBuilder();
        PrometheusWriteContext ctx = new PrometheusWriteContext(buffer, 1_700_000_000L);
        ctx.gauge("monitor_platform_users", 12);
        ctx.counter("monitor_platform_ingests_total", 5);

        HistogramAccumulator histogram = new HistogramAccumulator(HistogramAccumulator.DURATION_BUCKETS);
        histogram.observe(0.1);
        ctx.histogram("monitor_platform_ingest_duration_seconds", histogram, Map.of());

        String output = buffer.toString();
        assertTrue(output.contains("monitor_platform_users 12"));
        assertTrue(output.contains("monitor_platform_ingests_total 5"));
        assertTrue(output.contains("monitor_platform_ingest_duration_seconds_bucket{le=\"0.1\"}"));
        assertTrue(output.contains("monitor_platform_ingest_duration_seconds_sum"));
        assertTrue(output.contains("monitor_platform_ingest_duration_seconds_count"));
    }
}
