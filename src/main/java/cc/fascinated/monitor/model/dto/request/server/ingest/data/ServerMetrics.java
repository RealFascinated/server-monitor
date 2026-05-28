package cc.fascinated.monitor.model.dto.request.server.ingest.data;

public record ServerMetrics(
        Double cpuUsage,
        Double memoryUsage,
        Double memoryAvailable,
        Double memoryTotal,
        Double load1,
        Double load5,
        Double load15,
        Double cpuUserPercent,
        Double cpuSystemPercent,
        Double cpuIowaitPercent,
        Double cpuStealPercent,
        Long memoryBuffers,
        Long memoryCached,
        Long swapUsed,
        Long swapTotal,
        Long processCount,
        Long runningProcesses,
        Long contextSwitchesPerSecond,
        Long interruptsPerSecond
) {}
