package cc.fascinated.monitor.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ClientRequestUtils {
    private static final int MAX_USER_AGENT_LENGTH = 512;

    public static String clientIp(HttpServletRequest request) {
        String cloudflareIp = headerValue(request, "CF-Connecting-IP");
        if (cloudflareIp != null) {
            return cloudflareIp;
        }

        String forwarded = headerValue(request, "X-Forwarded-For");
        if (forwarded != null) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }

        String realIp = headerValue(request, "X-Real-IP");
        if (realIp != null) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    private static String headerValue(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static String userAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        if (userAgent.length() <= MAX_USER_AGENT_LENGTH) {
            return userAgent;
        }
        return userAgent.substring(0, MAX_USER_AGENT_LENGTH);
    }
}
