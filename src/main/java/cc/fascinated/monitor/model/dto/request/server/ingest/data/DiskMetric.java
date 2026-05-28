package cc.fascinated.monitor.model.dto.request.server.ingest.data;

public record DiskMetric(
        String diskName,
        long usedBytes,
        long totalBytes,
        long ioReadBytesPerSecond,
        double ioUsagePercent,
        double ioWaitMilliseconds
) {}
