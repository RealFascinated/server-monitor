package cc.fascinated.monitor.model.persistance.metric;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "server_network_metrics")
@NoArgsConstructor
@Getter
public class ServerInterfaceMetricRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id")
    private Long serverId;

    @Column(name = "interface_name")
    private String interfaceName;

    @Column(name = "rx_bps")
    private long rxBytesPerSecond;

    @Column(name = "tx_bps")
    private long txBytesPerSecond;

    @Column(name = "rx_packets_per_second")
    private long rxPacketsPerSecond;

    @Column(name = "tx_packets_per_second")
    private long txPacketsPerSecond;

    @Column(name = "rx_errors_per_second")
    private long rxErrorsPerSecond;

    @Column(name = "tx_errors_per_second")
    private long txErrorsPerSecond;

    @Column(name = "timestamp")
    private Instant timestamp;

    public ServerInterfaceMetricRow(Long serverId, String interfaceName, long rxBytesPerSecond, long txBytesPerSecond, long rxPacketsPerSecond,
                                    long txPacketsPerSecond, long rxErrorsPerSecond, long txErrorsPerSecond, Instant timestamp) {
        this.serverId = serverId;
        this.interfaceName = interfaceName;
        this.rxBytesPerSecond = rxBytesPerSecond;
        this.txBytesPerSecond = txBytesPerSecond;
        this.rxPacketsPerSecond = rxPacketsPerSecond;
        this.txPacketsPerSecond = txPacketsPerSecond;
        this.rxErrorsPerSecond = rxErrorsPerSecond;
        this.txErrorsPerSecond = txErrorsPerSecond;
        this.timestamp = timestamp;
    }
}
