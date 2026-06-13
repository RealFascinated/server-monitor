package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import org.jetbrains.annotations.Nullable;

import java.util.List;

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
        Long interruptsPerSecond,
        Long fdOpen,
        Long fdMax,
        Double fdUsagePercent,
        Long oomKillsTotal,
        Long oomKillsPerSecond,
        Double cpuPowerWatts,
        @Nullable List<CpuCoreMetric> cpuCoreMetrics,
        @Nullable List<TemperatureMetric> temperatureMetrics
) {}
