package cc.fascinated.monitor.model.dto.response.server;

import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.domain.server.ServerRole;
import cc.fascinated.monitor.model.persistance.ServerRow;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public record ServerResponse(
        long serverId,
        String serverName,
        ServerStatus status,
        Long uptimeSeconds,
        String agentVersion,
        Instant createdAt,
        Instant lastHeartbeat,
        Double uptimePercent30d,
        ServerRole role,
        int permissions,
        String folderName,
        ServerInventoryResponse inventory,
        @Nullable ServerCpuSnapshot cpu,
        @Nullable ServerMemorySnapshot memory,
        @Nullable ServerDiskSnapshot disk
) {
    public static ServerResponse from(
            ServerRow server,
            ServerRole role,
            @Nullable ServerCpuSnapshot cpu,
            @Nullable ServerMemorySnapshot memory,
            @Nullable ServerDiskSnapshot disk,
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
                uptimePercent30d,
                role,
                role.permissionMask(),
                folderName,
                server.getInventory() != null
                        ? ServerInventoryResponse.from(server.getInventory())
                        : null,
                cpu,
                memory,
                disk
        );
    }
}
