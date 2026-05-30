package cc.fascinated.monitor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "monitor.servers")
public class MonitorServerProperties {
    private Duration offlineThreshold = Duration.ofMinutes(2);
}
