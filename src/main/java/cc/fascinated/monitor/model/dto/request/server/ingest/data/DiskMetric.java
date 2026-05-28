package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.NotBlank;

public record DiskMetric(
        @NotBlank(message = "Disk name must not be empty")
        String diskName,
        long usedBytes,
        long totalBytes,
        long ioReadBytesPerSecond,
        long ioWriteBytesPerSecond,
        double ioUsagePercent,
        double ioWaitMilliseconds
) {}
