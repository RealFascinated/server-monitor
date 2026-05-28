package cc.fascinated.monitor.model.persistance;

import cc.fascinated.monitor.model.domain.server.ServerStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "servers")
@NoArgsConstructor
@Getter
@Setter
public class ServerRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "server_name", nullable = false)
    private String serverName;

    @Column(name = "ip")
    private String ip;

    @Column(name = "core_count")
    private int coreCount;

    @Column(name = "thread_count")
    private int threadCount;

    @Column(name = "cpu_model")
    private String cpuModel;

    @Column(name = "socket_count")
    private int socketCount;

    @Column(name = "cpu_clock_mhz")
    private double cpuClockMhz;

    @Column(name = "os_name")
    private String osName;

    @Column(name = "os_version")
    private String osVersion;

    @Column(name = "status")
    private ServerStatus status;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Column(name = "last_uptime_seconds")
    private long lastUptimeSeconds;

    @Column(name = "created_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    public ServerRow(String serverName, Instant createdAt) {
        this.serverName = serverName;
        this.createdAt = createdAt;
    }
}