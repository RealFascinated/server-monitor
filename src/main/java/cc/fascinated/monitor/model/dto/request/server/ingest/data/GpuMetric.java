package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jetbrains.annotations.Nullable;

public record GpuMetric(
        @NotBlank(message = "GPU device id must not be empty")
        String deviceId,
        @NotBlank(message = "GPU name must not be empty")
        String name,
        @Nullable String vendor,
        @NotNull(message = "GPU usage must not be null")
        Double usagePercent,
        @Nullable Double encoderUsagePercent,
        @Nullable Double decoderUsagePercent,
        @Nullable Long memoryUsedBytes,
        @Nullable Long memoryTotalBytes,
        @Nullable Double temperatureCelsius,
        @Nullable Double powerWatts
) {}
