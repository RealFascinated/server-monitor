package cc.fascinated.monitor.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.experimental.UtilityClass;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serialises tool results the way the REST API does: ISO dates and no null fields. Spring AI's own converter writes
 * every null, which spends a model's context on fields that say nothing. A tool that returns the resulting String
 * is passed to the client as text unchanged.
 */
@UtilityClass
public class McpJson {
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .changeDefaultPropertyInclusion(inclusion ->
                    JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
            .build();

    public static String write(Object value) {
        return MAPPER.writeValueAsString(value);
    }
}
