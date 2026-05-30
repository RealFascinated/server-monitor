package cc.fascinated.monitor.metrics.config;

import cc.fascinated.monitor.config.MonitorServerProperties;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(MonitorServerProperties.class)
public class PrometheusMetricsConfig {

    @Bean(name = "monitorMetricsProperties")
    @ConfigurationProperties(prefix = "monitor.metrics")
    public MonitorMetricsProperties monitorMetricsProperties() {
        return new MonitorMetricsProperties();
    }

    @Bean
    public PrometheusRegistry prometheusRegistry() {
        return new PrometheusRegistry();
    }
}
