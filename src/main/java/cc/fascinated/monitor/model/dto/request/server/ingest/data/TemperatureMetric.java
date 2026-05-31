package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TemperatureMetric(
        @NotBlank(message = "Sensor name must not be empty")
        String sensor,
        @NotNull(message = "Temperature must not be null")
        Double celsius
) {}
