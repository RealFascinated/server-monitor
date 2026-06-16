package cc.fascinated.monitor.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MonitorProperties.class)
public class MonitorConfig {
}
