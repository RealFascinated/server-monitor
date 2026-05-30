package cc.fascinated.monitor.metrics.counter;

import cc.fascinated.monitor.metrics.holder.CounterHolder;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.stereotype.Component;

@Component
public class IngestAuthFailuresCounterMetric {
    private final CounterHolder counter;

    public IngestAuthFailuresCounterMetric(PrometheusRegistry registry) {
        this.counter = CounterHolder.register(registry, "monitor_ingest_auth_failures", "Failed ingest authentication attempts");
    }

    public void recordFailure() {
        this.counter.increment();
    }
}
