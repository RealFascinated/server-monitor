package cc.fascinated.monitor.metrics.platform.collector;

import cc.fascinated.monitor.util.HttpPathNormalizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnProperty(prefix = "monitor.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class HttpMetricsFilter extends OncePerRequestFilter {
    private final PlatformMetricsRecorder platformMetricsRecorder;

    public HttpMetricsFilter(PlatformMetricsRecorder platformMetricsRecorder) {
        this.platformMetricsRecorder = platformMetricsRecorder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            double durationSeconds = (System.nanoTime() - startedNanos) / 1_000_000_000.0;
            this.platformMetricsRecorder.recordHttpRequest(
                    request.getMethod(),
                    HttpPathNormalizer.normalize(request.getRequestURI()),
                    response.getStatus(),
                    durationSeconds
            );
        }
    }
}
