package cc.fascinated.monitor.metrics.vm;

import java.util.Map;

/** Observes every gauge written through a {@link MetricWriteContext}, with its merged labels (including server_id). */
@FunctionalInterface
public interface MetricSink {
    void accept(String metricName, double value, Map<String, String> labels);
}
