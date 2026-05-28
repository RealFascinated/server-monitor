package cc.fascinated.monitor.model.persistance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "server_disk_metrics")
@NoArgsConstructor
@Getter
public class ServerDiskMetricRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "timestamp")
    private Instant timestamp;

    public ServerDiskMetricRow(Long serverId, String diskName, double usagePercent, long usedBytes, long totalBytes,
                               long ioReadBytesPerSecond, long ioWriteBytesPerSecond, double ioUsagePercent,
                               double ioWaitMilliseconds, Instant timestamp) {
        this.serverId = serverId;
        this.diskName = diskName;
        this.usagePercent = usagePercent;
        this.usedBytes = usedBytes;
        this.totalBytes = totalBytes;
        this.ioReadBytesPerSecond = ioReadBytesPerSecond;
        this.ioWriteBytesPerSecond = ioWriteBytesPerSecond;
        this.ioUsagePercent = ioUsagePercent;
        this.ioWaitMilliseconds = ioWaitMilliseconds;
        this.timestamp = timestamp;
    }
}