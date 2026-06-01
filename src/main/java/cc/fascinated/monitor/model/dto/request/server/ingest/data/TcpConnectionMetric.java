package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TcpConnectionMetric(
        @NotBlank(message = "TCP state must not be empty")
        String state,
        @NotNull(message = "TCP connection count must not be null")
        @Min(value = 0, message = "TCP connection count must be at least 0")
        Long count
) {}
