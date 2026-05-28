package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.NotBlank;

public record ServerDetails(
        @NotBlank(message = "IP must not be empty")
        String ip,
        Integer coreCount,
        Integer threadCount,
        @NotBlank(message = "OS name must not be empty")
        String osName,
        @NotBlank(message = "OS version must not be empty")
        String osVersion,
        Long uptimeSeconds,
        @NotBlank(message = "CPU model must not be empty")
        String cpuModel,
        Integer socketCount,
        Double cpuClockMhz
) {}
