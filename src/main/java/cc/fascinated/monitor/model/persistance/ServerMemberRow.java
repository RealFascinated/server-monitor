package cc.fascinated.monitor.model.persistance;

import cc.fascinated.monitor.model.domain.server.ServerMemberRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "server_members")
@IdClass(ServerMemberId.class)
@NoArgsConstructor
@Getter
@Setter
public class ServerMemberRow {
    @Id
    @Column(name = "server_id", nullable = false)
    private Long serverId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServerMemberRole role;

    @Column(name = "created_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    public ServerMemberRow(Long serverId, Long userId, ServerMemberRole role, Instant createdAt) {
        this.serverId = serverId;
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
    }
}
