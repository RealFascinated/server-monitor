package cc.fascinated.monitor.metrics.vm.write;

import cc.fascinated.monitor.metrics.shared.support.HistogramAccumulator;
import cc.fascinated.monitor.metrics.vm.query.Promql;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public final class PrometheusWriteContext {
    private final StringBuilder buffer;
    private final long epochSeconds;

    public void gauge(String metricName, double value) {
        gauge(metricName, value, Map.of());
    }

    public void gauge(String metricName, double value, Map<String, String> labels) {
        appendMetric(metricName, labels);
        appendSampleValue(value);
    }

    public void counter(String metricName, long value) {
        counter(metricName, value, Map.of());
    }

    public void counter(String metricName, long value, Map<String, String> labels) {
        appendMetric(metricName, labels);
        appendSampleValue(value);
    }

    public void histogram(String metricName, HistogramAccumulator histogram, Map<String, String> labels) {
        double[] buckets = histogram.buckets();
        for (int index = 0; index < buckets.length; index++) {
            appendHistogramBucket(metricName, labels, String.valueOf(buckets[index]), histogram.bucketCount(index));
        }
        appendHistogramBucket(metricName, labels, "+Inf", histogram.bucketCount(buckets.length));
        gauge(metricName + "_sum", histogram.sum(), labels);
        counter(metricName + "_count", histogram.count(), labels);
    }

    private void appendHistogramBucket(String metricName, Map<String, String> labels, String le, long count) {
        appendMetric(metricName + "_bucket", labels, Map.entry("le", le));
        appendSampleValue(count);
    }

    private void appendMetric(String metricName, Map<String, String> labels) {
        appendMetric(metricName, labels, null);
    }

    private void appendMetric(String metricName, Map<String, String> labels, Map.Entry<String, String> extraLabel) {
        this.buffer.append(metricName);
        if (labels.isEmpty() && extraLabel == null) {
            return;
        }
        this.buffer.append('{');
        boolean first = true;
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            if (!first) {
                this.buffer.append(',');
            }
            appendLabel(entry.getKey(), entry.getValue());
            first = false;
        }
        if (extraLabel != null) {
            if (!first) {
                this.buffer.append(',');
            }
            appendLabel(extraLabel.getKey(), extraLabel.getValue());
        }
        this.buffer.append('}');
    }

    private void appendLabel(String key, String value) {
        this.buffer.append(key);
        this.buffer.append("=\"");
        Promql.appendEscapedLabelValue(this.buffer, value);
        this.buffer.append('"');
    }

    private void appendSampleValue(double value) {
        this.buffer.append(' ');
        this.buffer.append(value);
        this.buffer.append(' ');
        this.buffer.append(this.epochSeconds);
        this.buffer.append('\n');
    }

    private void appendSampleValue(long value) {
        this.buffer.append(' ');
        this.buffer.append(value);
        this.buffer.append(' ');
        this.buffer.append(this.epochSeconds);
        this.buffer.append('\n');
    }
}
