package cc.fascinated.monitor.model.persistance.metric;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "server_zfs_pool_metrics")
@IdClass(MetricRowId.class)
@NoArgsConstructor
@Getter
public class ZfsPoolMetricRow {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "server_zfs_pool_metrics_id_seq")
    @SequenceGenerator(name = "server_zfs_pool_metrics_id_seq", sequenceName = "server_zfs_pool_metrics_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "server_id")
    private Long serverId;

    @Column(name = "pool_name")
    private String poolName;

    @Column(name = "health")
    private String health;

    @Column(name = "capacity_percent")
    private double capacityPercent;

    @Column(name = "allocated_bytes")
    private long allocatedBytes;

    @Column(name = "free_bytes")
    private long freeBytes;

    @Column(name = "total_bytes")
    private long totalBytes;

    @Column(name = "fragmentation_percent")
    private double fragmentationPercent;

    @Column(name = "scan_state")
    private String scanState;

    @Column(name = "scan_percent")
    private double scanPercent;

    @Column(name = "read_bps")
    private long readBps;

    @Column(name = "write_bps")
    private long writeBps;

    @Column(name = "read_iops")
    private long readIops;

    @Column(name = "write_iops")
    private long writeIops;

    @Column(name = "checksum_errors")
    private long checksumErrors;

    @Id
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public ZfsPoolMetricRow(
            Long serverId,
            String poolName,
            String health,
            double capacityPercent,
            long allocatedBytes,
            long freeBytes,
            long totalBytes,
            double fragmentationPercent,
            String scanState,
            double scanPercent,
            long readBps,
            long writeBps,
            long readIops,
            long writeIops,
            long checksumErrors,
            Instant timestamp
    ) {
        this.serverId = serverId;
        this.poolName = poolName;
        this.health = health;
        this.capacityPercent = capacityPercent;
        this.allocatedBytes = allocatedBytes;
        this.freeBytes = freeBytes;
        this.totalBytes = totalBytes;
        this.fragmentationPercent = fragmentationPercent;
        this.scanState = scanState;
        this.scanPercent = scanPercent;
        this.readBps = readBps;
        this.writeBps = writeBps;
        this.readIops = readIops;
        this.writeIops = writeIops;
        this.checksumErrors = checksumErrors;
        this.timestamp = timestamp;
    }
}
