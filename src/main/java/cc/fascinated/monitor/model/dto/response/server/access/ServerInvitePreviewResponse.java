package cc.fascinated.monitor.model.dto.response.server.access;

import cc.fascinated.monitor.model.domain.server.ServerRole;
import cc.fascinated.monitor.model.persistance.ServerInviteRow;
import cc.fascinated.monitor.model.persistance.ServerRow;
import cc.fascinated.monitor.model.persistance.UserRow;

import java.time.Instant;

public record ServerInvitePreviewResponse(
        String serverName,
        ServerRole role,
        String email,
        String invitedByEmail,
        Instant expiresAt
) {
    public static ServerInvitePreviewResponse from(
            ServerInviteRow invite,
            ServerRow server,
            UserRow invitedBy
    ) {
        return new ServerInvitePreviewResponse(
                server.getServerName(),
                invite.getRole(),
                invite.getEmail(),
                invitedBy.getEmail(),
                invite.getExpiresAt()
        );
    }
}
