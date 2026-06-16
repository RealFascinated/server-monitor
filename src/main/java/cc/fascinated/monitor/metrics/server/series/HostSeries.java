package cc.fascinated.monitor.metrics.server.series;

import cc.fascinated.monitor.metrics.vm.MetricWriteContext;
import cc.fascinated.monitor.metrics.server.catalog.MetricFamily;
import cc.fascinated.monitor.metrics.server.catalog.VmMetricFamily;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerDetails;
import cc.fascinated.monitor.model.dto.request.server.ingest.data.ServerMetrics;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum HostSeries implements VmMetricFamily {
    CPU_USAGE("monitor_host_cpu_usage"),
    MEM_USAGE("monitor_host_mem_usage"),
    MEM_AVAILABLE("monitor_host_mem_available"),
    MEM_TOTAL("monitor_host_mem_total"),
    LOAD_1("monitor_host_load_1"),
    LOAD_5("monitor_host_load_5"),
    LOAD_15("monitor_host_load_15"),
    CPU_USER_PCT("monitor_host_cpu_user_pct"),
    CPU_SYSTEM_PCT("monitor_host_cpu_system_pct"),
    CPU_IOWAIT_PCT("monitor_host_cpu_iowait_pct"),
    CPU_STEAL_PCT("monitor_host_cpu_steal_pct"),
    MEM_BUFFERS("monitor_host_mem_buffers"),
    MEM_CACHED("monitor_host_mem_cached"),
    SWAP_USED("monitor_host_swap_used"),
    SWAP_TOTAL("monitor_host_swap_total"),
    PROCESS_COUNT("monitor_host_process_count"),
    RUNNING_PROCESSES("monitor_host_running_processes"),
    CTX_SWITCHES_PER_SECOND("monitor_host_ctx_switches_per_second"),
    INTERRUPTS_PER_SECOND("monitor_host_interrupts_per_second"),
    FD_OPEN("monitor_host_fd_open"),
    FD_MAX("monitor_host_fd_max"),
    FD_USAGE_PCT("monitor_host_fd_usage_pct"),
    OOM_KILLS_TOTAL("monitor_host_oom_kills_total"),
    OOM_KILLS_PER_SECOND("monitor_host_oom_kills_per_second"),
    CPU_CLOCK_MHZ("monitor_host_cpu_clock_mhz"),
    CPU_POWER_WATTS("monitor_host_cpu_power_watts"),
    BATTERY_PCT("monitor_host_battery_pct");

    private final String metricName;

    HostSeries(String metricName) {
        this.metricName = metricName;
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
    }
}
