package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.NotBlank;

public record InterfaceMetrics(
        @NotBlank(message = "Interface name must not be empty")
        String interfaceName,
        Long rxBytesPerSecond,
        Long txBytesPerSecond,
        Long rxPacketsPerSecond,
        Long txPacketsPerSecond,
        Long rxErrorsPerSecond,
        Long txErrorsPerSecond
) {}
