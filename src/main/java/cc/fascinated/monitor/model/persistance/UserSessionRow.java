package cc.fascinated.monitor.model.persistance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "user_sessions")
@NoArgsConstructor
@Getter
public class UserSessionRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "ip_encrypted")
    private String ipEncrypted;

    @Column(name = "user_agent")
    private String userAgent;

    public UserSessionRow(
            Long userId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt,
            String ipEncrypted,
            String userAgent
    ) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.ipEncrypted = ipEncrypted;
        this.userAgent = userAgent;
    }
}
