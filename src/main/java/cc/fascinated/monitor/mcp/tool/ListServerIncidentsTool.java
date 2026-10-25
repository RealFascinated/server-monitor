package cc.fascinated.monitor.mcp.tool;

import cc.fascinated.monitor.mcp.McpJson;
import cc.fascinated.monitor.mcp.auth.McpAuth;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.dto.response.server.IncidentResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.server.ServerService;
import cc.fascinated.monitor.util.Pagination;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class ListServerIncidentsTool {
    private static final int DEFAULT_COUNT = 20;

    private final McpAuth auth;
    private final ServerService serverService;

    public ListServerIncidentsTool(McpAuth auth, ServerService serverService) {
        this.auth = auth;
        this.serverService = serverService;
    }

    @McpTool(name = "list_server_incidents", description = """
            Lists a server's outages, newest first: each incident is a period when the server stopped sending \
            heartbeats long enough to be marked OFFLINE, with startedAt and resolvedAt (absent while it is still \
            down). Use it for "when was this server down?" and post-mortems. Alerts that fired are in \
            list_alert_events instead. Returns a page: items, totalItems, itemsPerPage, totalPages.""")
    public String listServerIncidents(
            McpTransportContext context,
            @McpToolParam(description = "Server id from list_servers") long serverId,
            @McpToolParam(description = "Page number, starting at 1 (default 1)", required = false) Integer page,
            @McpToolParam(description = "Items per page, 1-50 (default 20)", required = false) Integer count
    ) {
        UserRow user = this.auth.require(context, ApiTokenScope.SERVERS_READ);
        return McpJson.write(this.serverService.listIncidents(user, serverId, page == null ? 1 : page,
                count == null ? DEFAULT_COUNT : count));
    }
}
