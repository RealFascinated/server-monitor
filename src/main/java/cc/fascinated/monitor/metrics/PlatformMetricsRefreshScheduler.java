package cc.fascinated.monitor.metrics;

import cc.fascinated.monitor.metrics.support.RefreshableMetric;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlatformMetricsRefreshScheduler {
    private final List<RefreshableMetric> refreshableMetrics;

    public PlatformMetricsRefreshScheduler(List<RefreshableMetric> refreshableMetrics) {
        this.refreshableMetrics = refreshableMetrics;
    }

    @Scheduled(fixedDelayString = "#{@monitorMetricsProperties.refreshIntervalMs}")
    public void refresh() {
        for (RefreshableMetric metric : this.refreshableMetrics) {
            metric.refresh();
        }
    }
}
