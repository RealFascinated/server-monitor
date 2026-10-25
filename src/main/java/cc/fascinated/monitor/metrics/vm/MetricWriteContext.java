package cc.fascinated.monitor.metrics.vm;

import cc.fascinated.monitor.metrics.vm.write.PrometheusWriteContext;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MetricWriteContext {
    private final PrometheusWriteContext writer;
    private final long serverId;
    private final Map<String, String> labels;
    @Nullable
    private final MetricSink sink;

    public MetricWriteContext(StringBuilder buffer, long serverId, long epochSeconds) {
        this(buffer, serverId, epochSeconds, null);
    }

    public MetricWriteContext(StringBuilder buffer, long serverId, long epochSeconds, @Nullable MetricSink sink) {
        this(new PrometheusWriteContext(buffer, epochSeconds), serverId, Map.of(), sink);
    }

    private MetricWriteContext(PrometheusWriteContext writer, long serverId, Map<String, String> labels,
                               @Nullable MetricSink sink) {
        this.writer = writer;
        this.serverId = serverId;
        this.labels = labels;
        this.sink = sink;
    }

    public MetricWriteContext withLabel(String key, String value) {
        Map<String, String> next = new LinkedHashMap<>(this.labels);
        next.put(key, value);
        return new MetricWriteContext(this.writer, this.serverId, next, this.sink);
    }

    public void gauge(String metricName, double value) {
        Map<String, String> merged = mergedLabels();
        this.writer.gauge(metricName, value, merged);
        if (this.sink != null) {
            this.sink.accept(metricName, value, merged);
        }
    }

    private Map<String, String> mergedLabels() {
        Map<String, String> merged = new LinkedHashMap<>();
        merged.put("server_id", Long.toString(this.serverId));
        merged.putAll(this.labels);
        return merged;
    }
}
