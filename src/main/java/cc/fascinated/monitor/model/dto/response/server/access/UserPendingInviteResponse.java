package cc.fascinated.monitor.model.dto.response.server.access;

import cc.fascinated.monitor.model.domain.server.ServerRole;
import cc.fascinated.monitor.model.persistance.ServerInviteRow;
import cc.fascinated.monitor.model.persistance.ServerRow;

import java.time.Instant;

public record UserPendingInviteResponse(
        long inviteId,
        long serverId,
        String serverName,
        ServerRole role,
        Instant expiresAt,
        Instant createdAt
) {
    public static UserPendingInviteResponse from(ServerInviteRow invite, ServerRow server) {
        return new UserPendingInviteResponse(
                invite.getId(),
                server.getId(),
                server.getServerName(),
                invite.getRole(),
                invite.getExpiresAt(),
                invite.getCreatedAt()
        );
    }
}
