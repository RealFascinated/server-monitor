package cc.fascinated.monitor.metrics.vm;

import cc.fascinated.monitor.metrics.vm.write.PrometheusWriteContext;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MetricWriteContext {
    private final PrometheusWriteContext writer;
    private final long serverId;
    private final Map<String, String> labels;

    public MetricWriteContext(StringBuilder buffer, long serverId, long epochSeconds) {
        this(new PrometheusWriteContext(buffer, epochSeconds), serverId, Map.of());
    }

    private MetricWriteContext(PrometheusWriteContext writer, long serverId, Map<String, String> labels) {
        this.writer = writer;
        this.serverId = serverId;
        this.labels = labels;
    }

    public MetricWriteContext withLabel(String key, String value) {
        Map<String, String> next = new LinkedHashMap<>(this.labels);
        next.put(key, value);
        return new MetricWriteContext(this.writer, this.serverId, next);
    }

    public void gauge(String metricName, double value) {
        this.writer.gauge(metricName, value, mergedLabels());
    }

    private Map<String, String> mergedLabels() {
        Map<String, String> merged = new LinkedHashMap<>();
        merged.put("server_id", Long.toString(this.serverId));
        merged.putAll(this.labels);
        return merged;
    }
}
