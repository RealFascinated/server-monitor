package cc.fascinated.monitor.model.dto.response.server.access;

import cc.fascinated.monitor.model.domain.server.ServerRole;
import cc.fascinated.monitor.model.persistance.ServerInviteRow;

import java.time.Instant;

public record PendingInviteResponse(
        long inviteId,
        String email,
        ServerRole role,
        Instant expiresAt,
        Instant createdAt
) {
    public static PendingInviteResponse from(ServerInviteRow invite) {
        return new PendingInviteResponse(
                invite.getId(),
                invite.getEmail(),
                invite.getRole(),
                invite.getExpiresAt(),
                invite.getCreatedAt()
        );
    }
}
