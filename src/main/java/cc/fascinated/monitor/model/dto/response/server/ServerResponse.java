package cc.fascinated.monitor.model.dto.response.server;

import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.persistance.ServerRow;

import java.time.Instant;

public record ServerResponse(
        long serverId,
        String serverName,
        ServerStatus status,
        Long uptimeSeconds,
        String agentVersion,
        Instant createdAt,
        Double cpuPercent,
        Long memUsage,
        Long memMax
) {
    public static ServerResponse from(ServerRow server, Double cpuPercent, Long memUsage, Long memMax) {
        return new ServerResponse(
                server.getId(),
                server.getServerName(),
                server.getStatus(),
                server.getLastUptimeSeconds(),
                server.getAgentVersion(),
                server.getCreatedAt(),
                cpuPercent,
                memUsage,
                memMax
        );
    }
}
