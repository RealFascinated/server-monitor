package cc.fascinated.monitor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "monitor.maxmind")
public class MonitorMaxMindProperties {
    private String license = "";
    private String databaseDir = "work/geoip";
}
