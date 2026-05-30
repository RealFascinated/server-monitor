package cc.fascinated.monitor.config;

import cc.fascinated.monitor.metrics.config.MonitorMetricsProperties;
import cc.fascinated.monitor.web.MetricsBearerAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsWebConfig {

    @Bean
    public FilterRegistrationBean<MetricsBearerAuthFilter> metricsBearerAuthFilter(MonitorMetricsProperties metricsProperties) {
        FilterRegistrationBean<MetricsBearerAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MetricsBearerAuthFilter(metricsProperties));
        registration.addUrlPatterns("/metrics");
        registration.setOrder(0);
        return registration;
    }
}
