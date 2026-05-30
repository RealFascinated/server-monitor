package cc.fascinated.monitor.model.persistance.metric;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "server_disk_metrics")
@IdClass(MetricRowId.class)
@NoArgsConstructor
@Getter
public class DiskMetricRow {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "server_disk_metrics_id_seq")
    @SequenceGenerator(name = "server_disk_metrics_id_seq", sequenceName = "server_disk_metrics_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "server_id")
    private Long serverId;

    @Column(name = "disk_name")
    private String diskName;

    @Column(name = "usage_pct")
    private double usagePercent;

    @Column(name = "used_bytes")
    private long usedBytes;

    @Column(name = "total_bytes")
    private long totalBytes;

    @Column(name = "io_read_bps")
    private long ioReadBytesPerSecond;

    @Column(name = "io_write_bps")
    private long ioWriteBytesPerSecond;

    @Column(name = "io_usage_pct")
    private double ioUsagePercent;

    @Column(name = "io_wait_ms")
    private double ioWaitMilliseconds;

    @Column(name = "inode_used")
    private long inodeUsed;

    @Column(name = "inode_total")
    private long inodeTotal;

    @Column(name = "read_iops")
    private long readIops;

    @Column(name = "write_iops")
    private long writeIops;

    @Column(name = "read_latency_ms")
    private long readLatencyMs;

    @Column(name = "write_latency_ms")
    private long writeLatencyMs;

    @Id
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public DiskMetricRow(Long serverId, String diskName, double usagePercent, long usedBytes, long totalBytes,
                         long ioReadBytesPerSecond, long ioWriteBytesPerSecond, double ioUsagePercent, double ioWaitMilliseconds,
                         long inodeUsed, long inodeTotal, long readIops, long writeIops, long readLatencyMs, long writeLatencyMs,
                         Instant timestamp) {
        this.serverId = serverId;
        this.diskName = diskName;
        this.usagePercent = usagePercent;
        this.usedBytes = usedBytes;
        this.totalBytes = totalBytes;
        this.ioReadBytesPerSecond = ioReadBytesPerSecond;
        this.ioWriteBytesPerSecond = ioWriteBytesPerSecond;
        this.ioUsagePercent = ioUsagePercent;
        this.ioWaitMilliseconds = ioWaitMilliseconds;
        this.inodeUsed = inodeUsed;
        this.inodeTotal = inodeTotal;
        this.readIops = readIops;
        this.writeIops = writeIops;
        this.readLatencyMs = readLatencyMs;
        this.writeLatencyMs = writeLatencyMs;
        this.timestamp = timestamp;
    }
}