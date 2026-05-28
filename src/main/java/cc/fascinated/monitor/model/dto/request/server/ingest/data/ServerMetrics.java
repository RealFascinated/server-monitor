package cc.fascinated.monitor.model.dto.request.server.ingest.data;

public record ServerMetrics(
        double cpuUsage,
        double memoryUsage,
        double memoryAvailable,
        double memoryTotal,
        double load1,
        double load5,
        double load15,
        double cpuUserPercent,
        double cpuSystemPercent,
        double cpuIowaitPercent,
        double cpuStealPercent,
        long memoryBuffers,
        long memoryCached,
        long swapUsed,
        long swapTotal,
        long processCount,
        long runningProcesses,
        long contextSwitchesPerSecond,
        long interruptsPerSecond
) {}
