package cc.fascinated.monitor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "monitor")
public class MonitorProperties {
    private String websiteUrl = "http://localhost:5173";
}
