package cc.fascinated.monitor.model.dto.response.server.access;

import cc.fascinated.monitor.model.domain.server.ServerMemberRole;
import cc.fascinated.monitor.model.persistance.ServerMemberRow;
import cc.fascinated.monitor.model.persistance.ServerRow;

import java.time.Instant;

public record ServerMemberResponse(
        long serverId,
        String serverName,
        ServerMemberRole role,
        Instant joinedAt
) {
    public static ServerMemberResponse from(ServerRow server, ServerMemberRow member) {
        return new ServerMemberResponse(
                server.getId(),
                server.getServerName(),
                member.getRole(),
                member.getCreatedAt()
        );
    }
}
