package cc.fascinated.monitor.model.dto.response.metrics;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetricsResponse(
        String range,
        Long step,
        List<Long> timestamps,
        @JsonIgnore Map<String, Object> sectionData
) {
    @JsonAnyGetter
    public Map<String, Object> sections() {
        return this.sectionData;
    }

    public static MetricsResponse empty(String range) {
        return new MetricsResponse(range, null, null, Map.of());
    }
}
