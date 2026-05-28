package cc.fascinated.monitor.model.persistance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "server_metrics")
@NoArgsConstructor
@Getter
public class ServerMetricRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id")
    private Long serverId;

    @Column(name = "cpu_usage")
    private double cpuUsage;

    @Column(name = "mem_usage")
    private double memoryUsage;

    @Column(name = "mem_total")
    private double memoryTotal;

    @Column(name = "timestamp")
    private Instant timestamp;

    public ServerMetricRow(Long serverId, double cpuUsage, double memoryUsage, double memoryTotal, Instant timestamp) {
        this.serverId = serverId;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.memoryTotal = memoryTotal;
        this.timestamp = timestamp;
    }
}
