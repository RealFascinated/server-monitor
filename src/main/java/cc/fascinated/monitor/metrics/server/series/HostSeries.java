package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerDetails;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerMetrics;
import cc.fascinated.monitor.model.domain.alert.AlertInfo;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import cc.fascinated.monitor.model.domain.alert.AlertUnit;
import org.jetbrains.annotations.Nullable;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum HostSeries implements VmMetricFamily {
    CPU_USAGE("monitor_host_cpu_usage",
            AlertInfo.of("CPU usage", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 90).asDefault(300, AlertSeverity.WARNING)),
    MEM_USAGE("monitor_host_mem_usage"),
    MEM_USAGE_PCT("monitor_host_mem_usage_pct",
            AlertInfo.of("Memory usage", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 90).asDefault(300, AlertSeverity.WARNING)),
    MEM_AVAILABLE("monitor_host_mem_available",
            AlertInfo.of("Available memory", AlertUnit.BYTES, AlertOperator.LESS_THAN, 536870912)),
    MEM_TOTAL("monitor_host_mem_total"),
    LOAD_1("monitor_host_load_1",
            AlertInfo.of("Load average (1m)", AlertUnit.COUNT, AlertOperator.MORE_THAN, 4)),
    LOAD_5("monitor_host_load_5",
            AlertInfo.of("Load average (5m)", AlertUnit.COUNT, AlertOperator.MORE_THAN, 4)),
    LOAD_15("monitor_host_load_15",
            AlertInfo.of("Load average (15m)", AlertUnit.COUNT, AlertOperator.MORE_THAN, 4)),
    CPU_USER_PCT("monitor_host_cpu_user_pct"),
    CPU_SYSTEM_PCT("monitor_host_cpu_system_pct",
            AlertInfo.of("CPU system time", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 50)),
    CPU_IOWAIT_PCT("monitor_host_cpu_iowait_pct",
            AlertInfo.of("CPU I/O wait", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 40)),
    CPU_STEAL_PCT("monitor_host_cpu_steal_pct",
            AlertInfo.of("CPU steal", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 20)),
    MEM_BUFFERS("monitor_host_mem_buffers"),
    MEM_CACHED("monitor_host_mem_cached"),
    SWAP_USED("monitor_host_swap_used",
            AlertInfo.of("Swap used", AlertUnit.BYTES, AlertOperator.MORE_THAN, 1073741824)),
    SWAP_TOTAL("monitor_host_swap_total"),
    PROCESS_COUNT("monitor_host_process_count"),
    RUNNING_PROCESSES("monitor_host_running_processes"),
    CTX_SWITCHES_PER_SECOND("monitor_host_ctx_switches_per_second"),
    INTERRUPTS_PER_SECOND("monitor_host_interrupts_per_second"),
    FD_OPEN("monitor_host_fd_open"),
    FD_MAX("monitor_host_fd_max"),
    FD_USAGE_PCT("monitor_host_fd_usage_pct",
            AlertInfo.of("File descriptor usage", AlertUnit.PERCENT, AlertOperator.MORE_THAN, 90)),
    OOM_KILLS_TOTAL("monitor_host_oom_kills_total"),
    OOM_KILLS_PER_SECOND("monitor_host_oom_kills_per_second",
            AlertInfo.of("Out-of-memory kills", AlertUnit.COUNT, AlertOperator.MORE_THAN, 0).asDefault(0, AlertSeverity.WARNING)),
    CPU_CLOCK_MHZ("monitor_host_cpu_clock_mhz"),
    CPU_POWER_WATTS("monitor_host_cpu_power_watts",
            AlertInfo.of("CPU power", AlertUnit.WATTS, AlertOperator.MORE_THAN, 150)),
    BATTERY_PCT("monitor_host_battery_pct",
            AlertInfo.of("Battery level", AlertUnit.PERCENT, AlertOperator.LESS_THAN, 20)),
    CLOCK_SYNCED("monitor_host_clock_synced",
            AlertInfo.of("Clock synchronisation", AlertUnit.STATE, AlertOperator.LESS_THAN, 1)),
    CLOCK_ERROR_MS("monitor_host_clock_error_ms",
            AlertInfo.of("Clock error", AlertUnit.MILLISECONDS, AlertOperator.MORE_THAN, 500)),
    CLOCK_OFFSET_MS("monitor_host_clock_offset_ms");

    private final String metricName;
    @Nullable
    private final AlertInfo alertInfo;

    HostSeries(String metricName) {
        this(metricName, null);
    }

    HostSeries(String metricName, @Nullable AlertInfo alertInfo) {
        this.metricName = metricName;
        this.alertInfo = alertInfo;
    }

    @Override
    public MetricFamily metricFamily() {
        return MetricFamily.HOST;
    }

    @Override
    public String metricPrefix() {
        return "monitor_host_";
    }

    public static void write(MetricWriteContext ctx, ServerMetrics metrics, ServerDetails details) {
        CPU_USAGE.writeNullable(ctx, metrics.cpuUsage());
        MEM_USAGE.writeNullable(ctx, metrics.memoryUsage());
        if (metrics.memoryUsage() != null && metrics.memoryTotal() != null && metrics.memoryTotal() > 0) {
            MEM_USAGE_PCT.write(ctx, metrics.memoryUsage() * 100.0 / metrics.memoryTotal());
        }
        MEM_AVAILABLE.writeNullable(ctx, metrics.memoryAvailable());
        MEM_TOTAL.writeNullable(ctx, metrics.memoryTotal());
        LOAD_1.writeNullable(ctx, metrics.load1());
        LOAD_5.writeNullable(ctx, metrics.load5());
        LOAD_15.writeNullable(ctx, metrics.load15());
        CPU_USER_PCT.writeNullable(ctx, metrics.cpuUserPercent());
        CPU_SYSTEM_PCT.writeNullable(ctx, metrics.cpuSystemPercent());
        CPU_IOWAIT_PCT.writeNullable(ctx, metrics.cpuIowaitPercent());
        CPU_STEAL_PCT.writeNullable(ctx, metrics.cpuStealPercent());
        MEM_BUFFERS.writeNullable(ctx, metrics.memoryBuffers());
        MEM_CACHED.writeNullable(ctx, metrics.memoryCached());
        SWAP_USED.writeNullable(ctx, metrics.swapUsed());
        SWAP_TOTAL.writeNullable(ctx, metrics.swapTotal());
        PROCESS_COUNT.writeNullable(ctx, metrics.processCount());
        RUNNING_PROCESSES.writeNullable(ctx, metrics.runningProcesses());
        CTX_SWITCHES_PER_SECOND.writeNullable(ctx, metrics.contextSwitchesPerSecond());
        INTERRUPTS_PER_SECOND.writeNullable(ctx, metrics.interruptsPerSecond());
        FD_OPEN.writeNullable(ctx, metrics.fdOpen());
        FD_MAX.writeNullable(ctx, metrics.fdMax());
        FD_USAGE_PCT.writeNullable(ctx, metrics.fdUsagePercent());
        OOM_KILLS_TOTAL.writeNullable(ctx, metrics.oomKillsTotal());
        OOM_KILLS_PER_SECOND.writeNullable(ctx, metrics.oomKillsPerSecond());
        CPU_CLOCK_MHZ.writeNullable(ctx, details.cpuClockMhz());
        CPU_POWER_WATTS.writeNullable(ctx, metrics.cpuPowerWatts());
        BATTERY_PCT.writeNullable(ctx, metrics.batteryPercent());
        CLOCK_SYNCED.writeNullable(ctx, metrics.clockSynced() == null ? null : (metrics.clockSynced() ? 1L : 0L));
        CLOCK_ERROR_MS.writeNullable(ctx, metrics.clockErrorMs());
        CLOCK_OFFSET_MS.writeNullable(ctx, metrics.clockOffsetMs());
    }
}
