package cc.fascinated.monitor.metrics.vm;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(VictoriaMetricsProperties.class)
public class VictoriaMetricsConfig {
}
