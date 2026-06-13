package cc.fascinated.monitor.model.dto.response.server;

import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.domain.server.ServerRole;
import cc.fascinated.monitor.model.persistance.ServerRow;

import java.time.Instant;

public record ServerResponse(
        long serverId,
        String serverName,
        ServerStatus status,
        Long uptimeSeconds,
        String agentVersion,
        Instant createdAt,
        Instant lastHeartbeat,
        Double cpuPercent,
        Long memUsage,
        Long memMax,
        Long diskUsage,
        Long diskMax,
        Double uptimePercent30d,
        ServerRole role,
        String folderName,
        ServerInventoryResponse inventory
) {
    public static ServerResponse from(
            ServerRow server,
            ServerRole role,
            Double cpuPercent,
            Long memUsage,
            Long memMax,
            Long diskUsage,
            Long diskMax,
            Double uptimePercent30d,
            String folderName
    ) {
        return new ServerResponse(
                server.getId(),
                server.getServerName(),
                server.getStatus(),
                server.getLastUptimeSeconds(),
                server.getAgentVersion(),
                server.getCreatedAt(),
                server.getLastHeartbeat(),
                cpuPercent,
                memUsage,
                memMax,
                diskUsage,
                diskMax,
                uptimePercent30d,
                role,
                folderName,
                server.getInventory() != null
                        ? ServerInventoryResponse.from(server.getInventory())
                        : null
        );
    }
}
