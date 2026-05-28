package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.NotBlank;

public record DiskMetric(
        @NotBlank(message = "Disk name must not be empty")
        String diskName,
        Long usedBytes,
        Long totalBytes,
        Long ioReadBytesPerSecond,
        Long ioWriteBytesPerSecond,
        Double ioUsagePercent,
        Double ioWaitMilliseconds,
        Long inodeUsed,
        Long inodeTotal,
        Long readIops,
        Long writeIops,
        Long readLatencyMs,
        Long writeLatencyMs
) {}
