package cc.fascinated.monitor.metrics.histogram;

import cc.fascinated.monitor.metrics.holder.HistogramHolder;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.stereotype.Component;

@Component
public class IngestDurationHistogramMetric {
    private final HistogramHolder histogram;

    public IngestDurationHistogramMetric(PrometheusRegistry registry) {
        this.histogram = HistogramHolder.register(registry, "monitor_ingest_duration_seconds", "Ingest request duration in seconds");
    }

    public void observeSeconds(double seconds) {
        this.histogram.observe(seconds);
    }
}
