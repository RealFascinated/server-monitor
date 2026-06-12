package cc.fascinated.monitor.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpPathNormalizerTest {
    @Test
    void normalize_replacesNumericSegments() {
        assertEquals("/v1/user/servers/{id}/metrics", HttpPathNormalizer.normalize("/v1/user/servers/42/metrics"));
        assertEquals("/v1/user/servers/{id}/metrics", HttpPathNormalizer.normalize("/v1/user/servers/99/metrics?range=1h"));
    }
}
