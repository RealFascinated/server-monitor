package cc.fascinated.monitor.model.persistance.metric;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "server_metrics")
@IdClass(MetricRowId.class)
@NoArgsConstructor
@Getter
public class ServerMetricRow {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "server_metrics_id_seq")
    @SequenceGenerator(name = "server_metrics_id_seq", sequenceName = "server_metrics_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "server_id")
    private Long serverId;

    @Column(name = "cpu_usage")
    private double cpuUsage;

    @Column(name = "mem_usage")
    private double memoryUsage;

    @Column(name = "mem_available")
    private double memoryAvailable;

    @Column(name = "mem_total")
    private double memoryTotal;

    @Column(name = "load_1")
    private double load1;

    @Column(name = "load_5")
    private double load5;

    @Column(name = "load_15")
    private double load15;

    @Column(name = "cpu_user_pct")
    private double cpuUserPercent;

    @Column(name = "cpu_system_pct")
    private double cpuSystemPercent;

    @Column(name = "cpu_iowait_pct")
    private double cpuIowaitPercent;

    @Column(name = "cpu_steal_pct")
    private double cpuStealPercent;

    @Column(name = "mem_buffers")
    private long memoryBuffers;

    @Column(name = "mem_cached")
    private long memoryCached;

    @Column(name = "swap_used")
    private long swapUsed;

    @Column(name = "swap_total")
    private long swapTotal;

    @Column(name = "process_count")
    private long processCount;

    @Column(name = "running_processes")
    private long runningProcesses;

    @Column(name = "ctx_switches_per_second")
    private long contextSwitchesPerSecond;

    @Column(name = "interrupts_per_second")
    private long interruptsPerSecond;

    @Id
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public ServerMetricRow(Long serverId, double cpuUsage, double memoryUsage, double memoryAvailable, double memoryTotal,
                           double load1, double load5, double load15, double cpuUserPercent, double cpuSystemPercent,
                           double cpuIowaitPercent, double cpuStealPercent, long memoryBuffers, long memoryCached, long swapUsed,
                           long swapTotal, long processCount, long runningProcesses, long contextSwitchesPerSecond,
                           long interruptsPerSecond, Instant timestamp) {
        this.serverId = serverId;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.memoryAvailable = memoryAvailable;
        this.memoryTotal = memoryTotal;
        this.load1 = load1;
        this.load5 = load5;
        this.load15 = load15;
        this.cpuUserPercent = cpuUserPercent;
        this.cpuSystemPercent = cpuSystemPercent;
        this.cpuIowaitPercent = cpuIowaitPercent;
        this.cpuStealPercent = cpuStealPercent;
        this.memoryBuffers = memoryBuffers;
        this.memoryCached = memoryCached;
        this.swapUsed = swapUsed;
        this.swapTotal = swapTotal;
        this.processCount = processCount;
        this.runningProcesses = runningProcesses;
        this.contextSwitchesPerSecond = contextSwitchesPerSecond;
        this.interruptsPerSecond = interruptsPerSecond;
        this.timestamp = timestamp;
    }
}
