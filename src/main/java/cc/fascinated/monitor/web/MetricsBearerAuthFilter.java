package cc.fascinated.monitor.web;

import cc.fascinated.monitor.metrics.config.MonitorMetricsProperties;
import cc.fascinated.monitor.util.AuthUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MetricsBearerAuthFilter extends OncePerRequestFilter {

    private final MonitorMetricsProperties metricsProperties;

    public MetricsBearerAuthFilter(MonitorMetricsProperties metricsProperties) {
        this.metricsProperties = metricsProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String bearerToken = this.metricsProperties.getBearerToken();
        if (bearerToken == null || bearerToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!AuthUtils.bearerTokensEqual(bearerToken, request.getHeader("Authorization"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("Unauthorized");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
