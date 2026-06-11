package cc.fascinated.monitor.model.dto.response.server.access;

import cc.fascinated.monitor.model.domain.server.ServerRole;
import cc.fascinated.monitor.model.persistance.ServerInviteRow;

import java.time.Instant;

public record ServerInviteCreatedResponse(
        long inviteId,
        String email,
        ServerRole role,
        Instant expiresAt,
        String token
) {
    public static ServerInviteCreatedResponse from(ServerInviteRow invite, String token) {
        return new ServerInviteCreatedResponse(
                invite.getId(),
                invite.getEmail(),
                invite.getRole(),
                invite.getExpiresAt(),
                token
        );
    }
}
