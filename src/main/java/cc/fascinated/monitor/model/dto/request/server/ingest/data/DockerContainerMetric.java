package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DockerContainerMetric(
        @NotBlank(message = "Container name must not be empty")
        String containerName,
        @Min(value = 0, message = "CPU usage must be greater than 0")
        @Max(value = 100, message = "CPU usage must be less than 100")
        Double cpuUsage,
        @Min(value = 0, message = "Memory usage must be greater than 0")
        Long memoryUsage
) {}
