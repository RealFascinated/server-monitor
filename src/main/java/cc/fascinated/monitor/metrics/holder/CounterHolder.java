package cc.fascinated.monitor.metrics.holder;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public final class CounterHolder {
    private final Counter counter;

    private CounterHolder(Counter counter) {
        this.counter = counter;
    }

    public static CounterHolder register(PrometheusRegistry registry, String name, String help, String... labelNames) {
        Counter.Builder builder = Counter.builder().name(name).help(help);
        if (labelNames.length > 0) {
            builder.labelNames(labelNames);
        }
        return new CounterHolder(builder.register(registry));
    }

    public void increment() {
        this.counter.inc();
    }

    public void increment(String... labelValues) {
        this.counter.labelValues(labelValues).inc();
    }

    public void increment(double amount, String... labelValues) {
        this.counter.labelValues(labelValues).inc(amount);
    }
}
