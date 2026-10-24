package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.Min;
import org.jetbrains.annotations.Nullable;

/**
 * Health of one physical disk as reported by smartctl. Which fields are present depends on the
 * disk type: SATA reports sector counts, NVMe reports wear and error counters.
 */
public record SmartMetric(
        String device,
        String model,
        @Nullable Boolean healthPassed,
        @Nullable Double temperatureCelsius,
        @Nullable @Min(0) Long powerOnHours,
        @Nullable @Min(0) Long reallocatedSectors,
        @Nullable @Min(0) Long pendingSectors,
        @Nullable @Min(0) Long percentageUsed,
        @Nullable @Min(0) Long availableSpare,
        @Nullable @Min(0) Long mediaErrors,
        @Nullable @Min(0) Long unsafeShutdowns,
        @Nullable @Min(0) Long dataWrittenBytes,
        @Nullable @Min(0) Long dataReadBytes
) {}
