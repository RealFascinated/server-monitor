package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.Min;
import org.jetbrains.annotations.Nullable;

/**
 * Health of one physical disk as reported by smartctl. Which fields are present depends on the
 * disk type: spinning disks report sector, cycle and interface counters, SATA SSDs report endurance,
 * NVMe reports wear and error counters.
 */
public record SmartMetric(
        String device,
        String model,
        @Nullable Boolean healthPassed,
        @Nullable Double temperatureCelsius,
        @Nullable @Min(0) Long powerOnHours,
        @Nullable @Min(0) Long powerCycleCount,
        @Nullable @Min(0) Long reallocatedSectors,
        @Nullable @Min(0) Long reallocatedEvents,
        @Nullable @Min(0) Long pendingSectors,
        @Nullable @Min(0) Long reportedUncorrect,
        @Nullable @Min(0) Long offlineUncorrectable,
        @Nullable @Min(0) Long udmaCrcErrors,
        @Nullable @Min(0) Long loadCycleCount,
        @Nullable @Min(0) Long startStopCounts,
        @Nullable @Min(0) Long powerOffRetracts,
        @Nullable @Min(0) Long heliumPercent,
        @Nullable @Min(0) Long errorLogEntries,
        @Nullable @Min(0) Long enduranceUsedPercent,
        @Nullable @Min(0) Long percentageUsed,
        @Nullable @Min(0) Long availableSpare,
        @Nullable @Min(0) Long mediaErrors,
        @Nullable @Min(0) Long unsafeShutdowns,
        @Nullable @Min(0) Long dataWrittenBytes,
        @Nullable @Min(0) Long dataReadBytes
) {}