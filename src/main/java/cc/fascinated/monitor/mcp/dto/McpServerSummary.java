package cc.fascinated.monitor.mcp.dto;

import cc.fascinated.monitor.model.domain.server.ServerStatus;
import cc.fascinated.monitor.model.dto.response.server.ServerResponse;
import org.jetbrains.annotations.Nullable;

/** The few fields needed to find a server and see whether it is up; the full {@link ServerResponse} is far larger. */
public record McpServerSummary(
        long serverId,
        String serverName,
        ServerStatus status,
        @Nullable String folderName
) {
    public static McpServerSummary from(ServerResponse server) {
        return new McpServerSummary(server.serverId(), server.serverName(), server.status(), server.folderName());
    }
}
