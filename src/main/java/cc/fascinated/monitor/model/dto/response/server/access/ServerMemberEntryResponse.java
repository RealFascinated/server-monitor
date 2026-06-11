package cc.fascinated.monitor.model.dto.response.server.access;

import cc.fascinated.monitor.model.domain.server.ServerMemberRole;
import cc.fascinated.monitor.model.persistance.ServerMemberRow;
import cc.fascinated.monitor.model.persistance.UserRow;

import java.time.Instant;

public record ServerMemberEntryResponse(
        long userId,
        String email,
        ServerMemberRole role,
        Instant joinedAt
) {
    public static ServerMemberEntryResponse from(ServerMemberRow member, UserRow user) {
        return new ServerMemberEntryResponse(user.getId(), user.getEmail(), member.getRole(), member.getCreatedAt());
    }
}
