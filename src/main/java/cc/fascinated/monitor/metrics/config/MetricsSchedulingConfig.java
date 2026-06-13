package cc.fascinated.monitor.metrics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class MetricsSchedulingConfig {

    @Bean
    @ConfigurationProperties(prefix = "monitor.metrics")
    public MonitorMetricsProperties monitorMetricsProperties() {
        return new MonitorMetricsProperties();
    }
}
