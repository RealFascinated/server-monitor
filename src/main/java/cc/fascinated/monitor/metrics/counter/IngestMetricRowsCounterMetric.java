package cc.fascinated.monitor.metrics.counter;

import cc.fascinated.monitor.metrics.holder.CounterHolder;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.stereotype.Component;

@Component
public class IngestMetricRowsCounterMetric {
    private final CounterHolder counter;

    public IngestMetricRowsCounterMetric(PrometheusRegistry registry) {
        this.counter = CounterHolder.register(registry, "monitor_ingest_rows", "Metric rows written per ingest", "table");
    }

    public void recordRows(String table, double count) {
        if (count > 0) {
            this.counter.increment(count, table);
        }
    }
}
