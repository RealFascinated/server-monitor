package cc.fascinated.monitor.model.dto.response.server;

import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.persistance.ServerInventoryRow;
import cc.fascinated.monitor.model.persistance.ServerRow;

import java.time.Instant;

public record ServerResponse(
        long serverId,
        String serverName,
        ServerStatus status,
        Instant lastUpdated,
        Long lastUptimeSeconds,
        String agentVersion,
        Instant createdAt,
        String ip,
        String osName,
        String osVersion
) {
    public static ServerResponse from(ServerRow server) {
        ServerInventoryRow inventory = server.getInventory();
        return new ServerResponse(
                server.getId(),
                server.getServerName(),
                server.getStatus(),
                server.getLastUpdated(),
                server.getLastUptimeSeconds(),
                server.getAgentVersion(),
                server.getCreatedAt(),
                inventory != null ? inventory.getIp() : null,
                inventory != null ? inventory.getOsName() : null,
                inventory != null ? inventory.getOsVersion() : null
        );
    }
}
