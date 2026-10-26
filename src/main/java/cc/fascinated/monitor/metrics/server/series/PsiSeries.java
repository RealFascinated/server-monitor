package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.PsiMetrics;
import org.jetbrains.annotations.Nullable;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Linux Pressure Stall Information: how much of the time tasks were stalled waiting on CPU,
 * memory or I/O. Only the 5-minute averages are alertable; they are the kernel's least noisy
 * window, and a rule that needs to react faster gets its own duration window.
 */
@Getter
@Accessors(fluent = true)
public enum PsiSeries implements VmMetricFamily {
    CPU_SOME_AVG10_PCT("monitor_psi_cpu_some_avg10_pct"),
    CPU_SOME_AVG60_PCT("monitor_psi_cpu_some_avg60_pct"),
    CPU_SOME_AVG300_PCT("monitor_psi_cpu_some_avg300_pct",
            AlertInfo.of("CPU stall pressure", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 50)),
    CPU_SOME_TOTAL_MS("monitor_psi_cpu_some_total_ms"),
    CPU_FULL_AVG10_PCT("monitor_psi_cpu_full_avg10_pct"),
    CPU_FULL_AVG60_PCT("monitor_psi_cpu_full_avg60_pct"),
    CPU_FULL_AVG300_PCT("monitor_psi_cpu_full_avg300_pct",
            AlertInfo.of("CPU full stall pressure", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 10)),
    CPU_FULL_TOTAL_MS("monitor_psi_cpu_full_total_ms"),
    MEMORY_SOME_AVG10_PCT("monitor_psi_memory_some_avg10_pct"),
    MEMORY_SOME_AVG60_PCT("monitor_psi_memory_some_avg60_pct"),
    MEMORY_SOME_AVG300_PCT("monitor_psi_memory_some_avg300_pct",
            AlertInfo.of("Memory stall pressure", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 25)),
    MEMORY_SOME_TOTAL_MS("monitor_psi_memory_some_total_ms"),
    MEMORY_FULL_AVG10_PCT("monitor_psi_memory_full_avg10_pct"),
    MEMORY_FULL_AVG60_PCT("monitor_psi_memory_full_avg60_pct"),
    MEMORY_FULL_AVG300_PCT("monitor_psi_memory_full_avg300_pct",
            AlertInfo.of("Memory full stall pressure", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 10)),
    MEMORY_FULL_TOTAL_MS("monitor_psi_memory_full_total_ms"),
    IO_SOME_AVG10_PCT("monitor_psi_io_some_avg10_pct"),
    IO_SOME_AVG60_PCT("monitor_psi_io_some_avg60_pct"),
    IO_SOME_AVG300_PCT("monitor_psi_io_some_avg300_pct",
            AlertInfo.of("I/O stall pressure", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 50)),
    IO_SOME_TOTAL_MS("monitor_psi_io_some_total_ms"),
    IO_FULL_AVG10_PCT("monitor_psi_io_full_avg10_pct"),
    IO_FULL_AVG60_PCT("monitor_psi_io_full_avg60_pct"),
    IO_FULL_AVG300_PCT("monitor_psi_io_full_avg300_pct",
            AlertInfo.of("I/O full stall pressure", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 25)),
    IO_FULL_TOTAL_MS("monitor_psi_io_full_total_ms");

    private final String metricName;
    @Nullable
    private final AlertInfo alertInfo;

    PsiSeries(String metricName) {
        this(metricName, null);
    }

    PsiSeries(String metricName, @Nullable AlertInfo alertInfo) {
        this.metricName = metricName;
        this.alertInfo = alertInfo;
    }

    @Override
    public MetricFamily metricFamily() {
        return MetricFamily.PSI;
    }

    @Override
    public String metricPrefix() {
        return "monitor_psi_";
    }

    public static void write(MetricWriteContext ctx, PsiMetrics psi) {
        CPU_SOME_AVG10_PCT.writeNullable(ctx, psi.cpuSomeAvg10Pct());
        CPU_SOME_AVG60_PCT.writeNullable(ctx, psi.cpuSomeAvg60Pct());
        CPU_SOME_AVG300_PCT.writeNullable(ctx, psi.cpuSomeAvg300Pct());
        CPU_SOME_TOTAL_MS.writeNullable(ctx, psi.cpuSomeTotalMs());
        CPU_FULL_AVG10_PCT.writeNullable(ctx, psi.cpuFullAvg10Pct());
        CPU_FULL_AVG60_PCT.writeNullable(ctx, psi.cpuFullAvg60Pct());
        CPU_FULL_AVG300_PCT.writeNullable(ctx, psi.cpuFullAvg300Pct());
        CPU_FULL_TOTAL_MS.writeNullable(ctx, psi.cpuFullTotalMs());
        MEMORY_SOME_AVG10_PCT.writeNullable(ctx, psi.memorySomeAvg10Pct());
        MEMORY_SOME_AVG60_PCT.writeNullable(ctx, psi.memorySomeAvg60Pct());
        MEMORY_SOME_AVG300_PCT.writeNullable(ctx, psi.memorySomeAvg300Pct());
        MEMORY_SOME_TOTAL_MS.writeNullable(ctx, psi.memorySomeTotalMs());
        MEMORY_FULL_AVG10_PCT.writeNullable(ctx, psi.memoryFullAvg10Pct());
        MEMORY_FULL_AVG60_PCT.writeNullable(ctx, psi.memoryFullAvg60Pct());
        MEMORY_FULL_AVG300_PCT.writeNullable(ctx, psi.memoryFullAvg300Pct());
        MEMORY_FULL_TOTAL_MS.writeNullable(ctx, psi.memoryFullTotalMs());
        IO_SOME_AVG10_PCT.writeNullable(ctx, psi.ioSomeAvg10Pct());
        IO_SOME_AVG60_PCT.writeNullable(ctx, psi.ioSomeAvg60Pct());
        IO_SOME_AVG300_PCT.writeNullable(ctx, psi.ioSomeAvg300Pct());
        IO_SOME_TOTAL_MS.writeNullable(ctx, psi.ioSomeTotalMs());
        IO_FULL_AVG10_PCT.writeNullable(ctx, psi.ioFullAvg10Pct());
        IO_FULL_AVG60_PCT.writeNullable(ctx, psi.ioFullAvg60Pct());
        IO_FULL_AVG300_PCT.writeNullable(ctx, psi.ioFullAvg300Pct());
        IO_FULL_TOTAL_MS.writeNullable(ctx, psi.ioFullTotalMs());
    }
}