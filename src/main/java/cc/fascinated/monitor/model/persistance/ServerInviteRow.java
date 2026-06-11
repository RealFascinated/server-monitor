package cc.fascinated.monitor.model.persistance;

import cc.fascinated.monitor.model.domain.server.ServerMemberRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "server_invites")
@NoArgsConstructor
@Getter
@Setter
public class ServerInviteRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "server_id", nullable = false)
    private Long serverId;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServerMemberRole role;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "invited_by_id", nullable = false)
    private Long invitedById;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    public ServerInviteRow(Long serverId, String email, ServerMemberRole role, String tokenHash,
                           Long invitedById, Instant expiresAt, Instant createdAt) {
        this.serverId = serverId;
        this.email = email;
        this.role = role;
        this.tokenHash = tokenHash;
        this.invitedById = invitedById;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }
}
