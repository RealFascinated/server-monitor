package cc.fascinated.monitor.model.persistance.metric;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "server_ingest_tokens")
@NoArgsConstructor
@Getter
@Setter
public class IngestTokenRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "server_id", nullable = false, unique = true)
    @Setter(AccessLevel.NONE)
    private Long serverId;

    public IngestTokenRow(String tokenHash, Long serverId) {
        this.tokenHash = tokenHash;
        this.serverId = serverId;
    }
}
