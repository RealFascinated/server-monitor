package cc.fascinated.monitor.model.persistance;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

@Entity
@Table(name = "api_tokens")
@NoArgsConstructor
@Getter
public class ApiTokenRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "token_prefix", nullable = false)
    private String tokenPrefix;

    /** Comma-separated {@code ApiTokenScope} ids. */
    @Column(nullable = false)
    private String scopes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    @Nullable
    private Instant expiresAt;

    @Column(name = "last_used_at")
    @Setter
    @Nullable
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    @Setter
    @Nullable
    private Instant revokedAt;

    public ApiTokenRow(Long userId, String name, String tokenHash, String tokenPrefix, String scopes,
                       Instant createdAt, @Nullable Instant expiresAt) {
        this.userId = userId;
        this.name = name;
        this.tokenHash = tokenHash;
        this.tokenPrefix = tokenPrefix;
        this.scopes = scopes;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}
