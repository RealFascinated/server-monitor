package cc.fascinated.monitor.metrics.platform;

import cc.fascinated.monitor.metrics.config.MonitorMetricsProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlatformMetricsPushScheduler {
    private final MonitorMetricsProperties monitorMetricsProperties;
    private final PlatformMetricsPusher platformMetricsPusher;

    public PlatformMetricsPushScheduler(MonitorMetricsProperties monitorMetricsProperties,
                                        PlatformMetricsPusher platformMetricsPusher) {
        this.monitorMetricsProperties = monitorMetricsProperties;
        this.platformMetricsPusher = platformMetricsPusher;
    }

    @Scheduled(fixedDelayString = "#{@monitorMetricsProperties.refreshIntervalMs}")
    public void push() {
        if (!this.monitorMetricsProperties.isEnabled()) {
            return;
        }
        this.platformMetricsPusher.push();
    }
}
