package cc.fascinated.monitor.model.dto.response.admin;

import java.util.List;
import java.util.Map;

public record PlatformMetricsResponse(
        String range,
        List<Long> timestamps,
        Map<String, Object> sections
) {}
