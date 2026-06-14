package cc.fascinated.monitor.model.dto.response.server;

import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.persistance.ServerRow;

import java.time.Instant;

public record ServerStatusResponse(
        long serverId,
        ServerStatus status,
        Instant lastHeartbeat,
        String agentVersion,
        boolean hasMetrics
) {
    public static ServerStatusResponse from(ServerRow server) {
        return new ServerStatusResponse(
                server.getId(),
                server.getStatus(),
                server.getLastHeartbeat(),
                server.getAgentVersion(),
                server.getLastUpdated() != null
        );
    }
}
