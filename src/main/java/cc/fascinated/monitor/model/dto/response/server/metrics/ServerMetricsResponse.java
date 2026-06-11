package cc.fascinated.monitor.model.dto.response.server.metrics;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServerMetricsResponse(
        long id,
        String range,
        Long step,
        List<Long> timestamps,
        @JsonIgnore Map<String, Object> sectionData
) {
    @JsonAnyGetter
    public Map<String, Object> sections() {
        return this.sectionData;
    }

    public static ServerMetricsResponse empty(long id, String range) {
        return new ServerMetricsResponse(id, range, null, null, Map.of());
    }
}
