package cc.fascinated.monitor.mcp.tool;

import cc.fascinated.monitor.mcp.McpJson;
import cc.fascinated.monitor.mcp.auth.McpAuth;
import cc.fascinated.monitor.mcp.auth.RequiresScope;
import cc.fascinated.monitor.mcp.dto.McpServerSummary;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.dto.response.server.ServerResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.server.ServerService;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class ListServersTool {
    private final McpAuth auth;
    private final ServerService serverService;

    public ListServersTool(McpAuth auth, ServerService serverService) {
        this.auth = auth;
        this.serverService = serverService;
    }

    @RequiresScope(ApiTokenScope.SERVERS_READ)
    @McpTool(name = "list_servers", description = """
            Lists the servers the user can see, sorted by name. Start here: serverId is what every other server, \
            metric and alert tool takes. Each server has status (ONLINE, OFFLINE, or PENDING = never reported), \
            lastHeartbeat, agentVersion, uptimeSeconds (host uptime), uptimePercent30d, role (OWNER or VIEWER), \
            folderName, hardware/OS inventory, and a current snapshot: cpu in percent (0-100), memory and disk (the \
            root filesystem) as usage/max in bytes. Pass compact=true for just serverId, serverName, status and \
            folderName, and nameContains to keep only servers whose name contains a text. For history use \
            query_metrics; for outages use list_server_incidents.""")
    public String listServers(
            McpTransportContext context,
            @McpToolParam(description = "Return only this server (still as a one-element list)", required = false) Long serverId,
            @McpToolParam(description = "Only servers whose name contains this text, ignoring case", required = false) String nameContains,
            @McpToolParam(description = "Only serverId, serverName, status and folderName per server (default false)", required = false) Boolean compact
    ) {
        UserRow user = this.auth.require(context, ApiTokenScope.SERVERS_READ);
        List<ServerResponse> servers = serverId != null
                ? List.of(this.serverService.getServer(user, serverId))
                : this.serverService.listServers(user);
        if (nameContains != null && !nameContains.isBlank()) {
            String needle = nameContains.trim().toLowerCase(Locale.ROOT);
            servers = servers.stream()
                    .filter(server -> server.serverName().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        }
        if (Boolean.TRUE.equals(compact)) {
            return McpJson.write(servers.stream().map(McpServerSummary::from).toList());
        }
        return McpJson.write(servers);
    }
}
