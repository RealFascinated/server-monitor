package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.NotBlank;

public record InterfaceMetrics(
        @NotBlank(message = "Interface name must not be empty")
        String interfaceName,
        long rxBytesPerSecond,
        long txBytesPerSecond,
        long rxPacketsPerSecond,
        long txPacketsPerSecond,
        long rxErrorsPerSecond,
        long txErrorsPerSecond
) {}
