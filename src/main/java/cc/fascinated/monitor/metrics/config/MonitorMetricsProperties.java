package cc.fascinated.monitor.metrics.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "monitor.metrics")
public class MonitorMetricsProperties {
    private String bearerToken = "";
    private Duration refreshInterval = Duration.ofSeconds(30);
    private Duration reportingThreshold = Duration.ofMinutes(5);

    public long getRefreshIntervalMs() {
        return this.refreshInterval.toMillis();
    }

    public String getReportingWindowLabel() {
        long seconds = this.reportingThreshold.getSeconds();
        if (seconds % 3600 == 0) {
            return (seconds / 3600) + "h";
        }
        if (seconds % 60 == 0) {
            return (seconds / 60) + "m";
        }
        return seconds + "s";
    }
}
