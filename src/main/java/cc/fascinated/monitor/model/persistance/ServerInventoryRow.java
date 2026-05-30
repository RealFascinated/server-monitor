package cc.fascinated.monitor.model.persistance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "server_inventory")
@NoArgsConstructor
@Getter
@Setter
public class ServerInventoryRow {
    @Id
    @Column(name = "server_id")
    private Long serverId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "server_id")
    private ServerRow server;

    @Column(name = "ip")
    private String ip;

    @Column(name = "core_count")
    private Integer coreCount;

    @Column(name = "thread_count")
    private Integer threadCount;

    @Column(name = "cpu_model")
    private String cpuModel;

    @Column(name = "socket_count")
    private Integer socketCount;

    @Column(name = "cpu_clock_mhz")
    private Double cpuClockMhz;

    @Column(name = "os_name")
    private String osName;

    @Column(name = "os_version")
    private String osVersion;

    public ServerInventoryRow(ServerRow server) {
        this.server = server;
    }
}
