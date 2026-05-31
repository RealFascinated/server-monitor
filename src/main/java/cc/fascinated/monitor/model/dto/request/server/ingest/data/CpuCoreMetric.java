package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CpuCoreMetric(
        @NotBlank(message = "CPU label must not be empty")
        String cpu,
        @NotNull(message = "CPU usage must not be null")
        @DecimalMin(value = "0", message = "CPU usage must be at least 0")
        @DecimalMax(value = "100", message = "CPU usage must be at most 100")
        Double usagePercent
) {}
