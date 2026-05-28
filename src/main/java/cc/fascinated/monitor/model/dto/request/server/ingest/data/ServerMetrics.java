package cc.fascinated.monitor.model.dto.request.server.ingest.data;

public record ServerMetrics(
        double cpuUsage,
        double memoryUsage,
        double memoryTotal
) {}
