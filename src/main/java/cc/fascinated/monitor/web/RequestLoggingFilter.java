package cc.fascinated.monitor.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            if (status >= 400) {
                long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
                String line = "%s %s %d %dms".formatted(request.getMethod(), requestUri(request), status, durationMs);
                if (status >= 500) {
                    log.error(line);
                } else {
                    log.warn(line);
                }
            }
        }
    }

    private static String requestUri(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + '?' + query;
    }
}
