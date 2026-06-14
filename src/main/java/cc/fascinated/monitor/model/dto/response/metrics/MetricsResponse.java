package cc.fascinated.monitor.model.dto.response.metrics;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetricsResponse(
        Long from,
        Long to,
        Long step,
        List<Long> timestamps,
        @JsonIgnore Map<String, Object> sectionData
) {
    @JsonAnyGetter
    public Map<String, Object> sections() {
        return this.sectionData;
    }

    public static MetricsResponse empty(long from, long to, long step) {
        return new MetricsResponse(from, to, step, null, Map.of());
    }
}
