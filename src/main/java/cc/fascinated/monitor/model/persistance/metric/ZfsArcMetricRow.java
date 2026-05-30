package cc.fascinated.monitor.model.persistance.metric;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "server_zfs_arc_metrics")
@IdClass(MetricRowId.class)
@NoArgsConstructor
@Getter
public class ZfsArcMetricRow {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "server_zfs_arc_metrics_id_seq")
    @SequenceGenerator(name = "server_zfs_arc_metrics_id_seq", sequenceName = "server_zfs_arc_metrics_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "server_id")
    private Long serverId;

    @Column(name = "arc_size_bytes")
    private long arcSizeBytes;

    @Column(name = "arc_target_bytes")
    private long arcTargetBytes;

    @Column(name = "arc_max_bytes")
    private long arcMaxBytes;

    @Column(name = "arc_min_bytes")
    private long arcMinBytes;

    @Column(name = "arc_data_bytes")
    private long arcDataBytes;

    @Column(name = "arc_metadata_bytes")
    private long arcMetadataBytes;

    @Column(name = "l2arc_size_bytes")
    private long l2arcSizeBytes;

    @Column(name = "arc_hit_ratio")
    private double arcHitRatio;

    @Column(name = "arc_misses_per_second")
    private long arcMissesPerSecond;

    @Id
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public ZfsArcMetricRow(
            Long serverId,
            long arcSizeBytes,
            long arcTargetBytes,
            long arcMaxBytes,
            long arcMinBytes,
            long arcDataBytes,
            long arcMetadataBytes,
            long l2arcSizeBytes,
            double arcHitRatio,
            long arcMissesPerSecond,
            Instant timestamp
    ) {
        this.serverId = serverId;
        this.arcSizeBytes = arcSizeBytes;
        this.arcTargetBytes = arcTargetBytes;
        this.arcMaxBytes = arcMaxBytes;
        this.arcMinBytes = arcMinBytes;
        this.arcDataBytes = arcDataBytes;
        this.arcMetadataBytes = arcMetadataBytes;
        this.l2arcSizeBytes = l2arcSizeBytes;
        this.arcHitRatio = arcHitRatio;
        this.arcMissesPerSecond = arcMissesPerSecond;
        this.timestamp = timestamp;
    }
}
