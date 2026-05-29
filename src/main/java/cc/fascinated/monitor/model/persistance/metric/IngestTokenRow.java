package cc.fascinated.monitor.model.persistance.metric;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "server_ingest_tokens")
@NoArgsConstructor
@Getter
public class IngestTokenRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash")
    private String tokenHash;

    /**
     * This value is FK'd to the server this is associated to.
     */
    @Column(name = "server_id")
    private Long serverId;

    public IngestTokenRow(String tokenHash, Long serverId) {
        this.tokenHash = tokenHash;
        this.serverId = serverId;
    }
}
