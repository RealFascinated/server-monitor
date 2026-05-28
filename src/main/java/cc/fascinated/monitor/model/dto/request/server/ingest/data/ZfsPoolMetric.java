package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.NotBlank;

public record ZfsPoolMetric(
        @NotBlank(message = "Pool name must not be empty")
        String poolName,
        @NotBlank(message = "Pool health must not be empty")
        String health,
        double capacityPercent,
        long allocatedBytes,
        long freeBytes,
        long totalBytes,
        double fragmentationPercent,
        @NotBlank(message = "Scan state must not be empty")
        String scanState,
        double scanPercent,
        long readBps,
        long writeBps,
        long readIops,
        long writeIops,
        long checksumErrors
) {}
