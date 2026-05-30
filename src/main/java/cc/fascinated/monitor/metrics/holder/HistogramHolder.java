package cc.fascinated.monitor.metrics.holder;

import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public final class HistogramHolder {
    private static final double[] INGEST_DURATION_BUCKETS = {
            0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10
    };

    private final Histogram histogram;

    private HistogramHolder(Histogram histogram) {
        this.histogram = histogram;
    }

    public static HistogramHolder register(PrometheusRegistry registry, String name, String help, String... labelNames) {
        Histogram.Builder builder = Histogram.builder()
                .name(name)
                .help(help)
                .classicUpperBounds(INGEST_DURATION_BUCKETS);
        if (labelNames.length > 0) {
            builder.labelNames(labelNames);
        }
        return new HistogramHolder(builder.register(registry));
    }

    public void observe(double value) {
        this.histogram.observe(value);
    }

    public void observe(double value, String... labelValues) {
        this.histogram.labelValues(labelValues).observe(value);
    }
}
