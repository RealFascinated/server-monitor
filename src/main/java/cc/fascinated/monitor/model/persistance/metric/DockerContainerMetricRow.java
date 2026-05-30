package cc.fascinated.monitor.model.persistance.metric;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "server_docker_container_metrics")
@IdClass(MetricRowId.class)
@NoArgsConstructor
@Getter
public class DockerContainerMetricRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id")
    private Long serverId;

    @Column(name = "container_name")
    private String containerName;

    @Column(name = "cpu_usage")
    private Double cpuUsage;

    @Column(name = "memory_usage")
    private Long memoryUsage;

    @Id
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public DockerContainerMetricRow(
            Long serverId,
            String containerName,
            Double cpuUsage,
            Long memoryUsage,
            Instant timestamp
    ) {
        this.serverId = serverId;
        this.containerName = containerName;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.timestamp = timestamp;
    }
}