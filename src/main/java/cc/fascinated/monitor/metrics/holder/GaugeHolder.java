package cc.fascinated.monitor.metrics.holder;

import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public final class GaugeHolder {
    private final Gauge gauge;

    private GaugeHolder(Gauge gauge) {
        this.gauge = gauge;
    }

    public static GaugeHolder register(PrometheusRegistry registry, String name, String help, String... labelNames) {
        Gauge.Builder builder = Gauge.builder().name(name).help(help);
        if (labelNames.length > 0) {
            builder.labelNames(labelNames);
        }
        return new GaugeHolder(builder.register(registry));
    }

    public void set(double value) {
        this.gauge.set(value);
    }

    public void set(double value, String... labelValues) {
        this.gauge.labelValues(labelValues).set(value);
    }
}
