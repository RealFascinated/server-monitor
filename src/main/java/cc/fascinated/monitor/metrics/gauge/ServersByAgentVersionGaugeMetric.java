package cc.fascinated.monitor.metrics.gauge;

import cc.fascinated.monitor.metrics.holder.GaugeHolder;
import cc.fascinated.monitor.metrics.support.RefreshableMetric;
import cc.fascinated.monitor.service.PlatformMetricsQueryService;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class ServersByAgentVersionGaugeMetric implements RefreshableMetric {
    private final GaugeHolder gauge;
    private final PlatformMetricsQueryService queries;
    private final Set<String> previousLabels = new HashSet<>();

    public ServersByAgentVersionGaugeMetric(PrometheusRegistry registry, PlatformMetricsQueryService queries) {
        this.gauge = GaugeHolder.register(registry, "monitor_servers_by_agent_version", "Servers grouped by agent version", "version");
        this.queries = queries;
    }

    @Override
    public void refresh() {
        Map<String, Long> counts = this.queries.serversByAgentVersion();
        Set<String> currentLabels = counts.keySet();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            this.gauge.set(entry.getValue(), entry.getKey());
        }
        for (String label : this.previousLabels) {
            if (!currentLabels.contains(label)) {
                this.gauge.set(0, label);
            }
        }
        this.previousLabels.clear();
        this.previousLabels.addAll(currentLabels);
    }
}
