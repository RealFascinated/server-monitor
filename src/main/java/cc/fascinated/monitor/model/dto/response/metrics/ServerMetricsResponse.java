package cc.fascinated.monitor.model.dto.response.metrics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServerMetricsResponse(
        long id,
        @JsonUnwrapped MetricsResponse metrics
) {
    public static ServerMetricsResponse empty(long id, String range) {
        return new ServerMetricsResponse(id, MetricsResponse.empty(range));
    }
}
