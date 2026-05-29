package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DockerContainerMetric(
        @NotBlank(message = "Container name must not be empty")
        String containerName,
        @NotNull(message = "CPU usage must not be null")
        @DecimalMin(value = "0", message = "CPU usage must be at least 0")
        @DecimalMax(value = "100", message = "CPU usage must be at most 100")
        BigDecimal cpuUsage,
        @Min(value = 0, message = "Memory usage must be at least 0")
        Long memoryUsage
) {}
