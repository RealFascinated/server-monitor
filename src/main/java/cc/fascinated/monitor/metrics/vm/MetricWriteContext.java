package cc.fascinated.monitor.metrics.vm;

import cc.fascinated.monitor.metrics.vm.query.Promql;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MetricWriteContext {
    private final StringBuilder buffer;
    private final long serverId;
    private final long epochSeconds;
    private final Map<String, String> labels;

    public MetricWriteContext(StringBuilder buffer, long serverId, long epochSeconds) {
        this(buffer, serverId, epochSeconds, Map.of());
    }

    private MetricWriteContext(StringBuilder buffer, long serverId, long epochSeconds, Map<String, String> labels) {
        this.buffer = buffer;
        this.serverId = serverId;
        this.epochSeconds = epochSeconds;
        this.labels = labels;
    }

    public MetricWriteContext withLabel(String key, String value) {
        Map<String, String> next = new LinkedHashMap<>(this.labels);
        next.put(key, value);
        return new MetricWriteContext(this.buffer, this.serverId, this.epochSeconds, next);
    }

    public void gauge(String metricName, double value) {
        this.buffer.append(metricName);
        this.buffer.append('{');
        this.buffer.append("server_id=\"");
        this.buffer.append(this.serverId);
        this.buffer.append('"');
        for (Map.Entry<String, String> entry : this.labels.entrySet()) {
            this.buffer.append(',');
            this.buffer.append(entry.getKey());
            this.buffer.append("=\"");
            Promql.appendEscapedLabelValue(this.buffer, entry.getValue());
            this.buffer.append('"');
        }
        this.buffer.append('}');
        this.buffer.append(' ');
        this.buffer.append(value);
        this.buffer.append(' ');
        this.buffer.append(this.epochSeconds);
        this.buffer.append('\n');
    }

}
