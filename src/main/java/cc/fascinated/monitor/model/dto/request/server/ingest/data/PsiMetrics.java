package cc.fascinated.monitor.model.dto.request.server.ingest.data;

/**
 * Linux Pressure Stall Information. Averages are the kernel's share of time the resource was
 * stalled over 10 seconds, 1 minute and 5 minutes; "some" means at least one task was stalled and
 * "full" means every non-idle task was. Totals are cumulative stall time since boot. Every field
 * is absent when the kernel runs without CONFIG_PSI, and the full fields are absent for CPU on
 * kernels that export only a some line for it.
 */
public record PsiMetrics(
        Double cpuSomeAvg10Pct,
        Double cpuSomeAvg60Pct,
        Double cpuSomeAvg300Pct,
        Double cpuSomeTotalMs,
        Double cpuFullAvg10Pct,
        Double cpuFullAvg60Pct,
        Double cpuFullAvg300Pct,
        Double cpuFullTotalMs,
        Double memorySomeAvg10Pct,
        Double memorySomeAvg60Pct,
        Double memorySomeAvg300Pct,
        Double memorySomeTotalMs,
        Double memoryFullAvg10Pct,
        Double memoryFullAvg60Pct,
        Double memoryFullAvg300Pct,
        Double memoryFullTotalMs,
        Double ioSomeAvg10Pct,
        Double ioSomeAvg60Pct,
        Double ioSomeAvg300Pct,
        Double ioSomeTotalMs,
        Double ioFullAvg10Pct,
        Double ioFullAvg60Pct,
        Double ioFullAvg300Pct,
        Double ioFullTotalMs
) {}