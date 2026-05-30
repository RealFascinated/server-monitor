package cc.fascinated.monitor.metrics.counter;

import cc.fascinated.monitor.metrics.holder.CounterHolder;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.stereotype.Component;

@Component
public class TotalIngestsCounterMetric {
    private final CounterHolder counter;

    public TotalIngestsCounterMetric(PrometheusRegistry registry) {
        this.counter = CounterHolder.register(registry, "monitor_ingests", "Total successful metric ingests");
    }

    public void recordIngest() {
        this.counter.increment();
    }
}
