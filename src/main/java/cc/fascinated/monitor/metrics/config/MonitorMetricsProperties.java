package cc.fascinated.monitor.metrics.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "monitor.metrics")
public class MonitorMetricsProperties {
    private boolean enabled = true;
    private Duration refreshInterval = Duration.ofSeconds(30);

    public long getRefreshIntervalMs() {
        return this.refreshInterval.toMillis();
    }
}
