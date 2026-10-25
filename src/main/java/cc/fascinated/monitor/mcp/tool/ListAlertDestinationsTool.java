package cc.fascinated.monitor.mcp.tool;

import cc.fascinated.monitor.alert.AlertDestinationService;
import cc.fascinated.monitor.mcp.McpJson;
import cc.fascinated.monitor.mcp.auth.McpAuth;
import cc.fascinated.monitor.mcp.auth.RequiresScope;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.dto.response.alert.AlertDestinationResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListAlertDestinationsTool {
    private final McpAuth auth;
    private final AlertDestinationService destinationService;

    public ListAlertDestinationsTool(McpAuth auth, AlertDestinationService destinationService) {
        this.auth = auth;
        this.destinationService = destinationService;
    }

    @RequiresScope(ApiTokenScope.ALERTS_READ)
    @McpTool(name = "list_alert_destinations", description = """
            Lists where alerts are delivered: id, name, type (DISCORD_WEBHOOK), maskedUrl (the secret part is \
            hidden) and groupCount, how many alert groups use it. Each group points at one destination (see \
            destinationId in list_alert_groups); a group without one notifies nobody. Destinations are created and \
            changed in the dashboard only.""")
    public String listAlertDestinations(McpTransportContext context) {
        UserRow user = this.auth.require(context, ApiTokenScope.ALERTS_READ);
        return McpJson.write(this.destinationService.list(user));
    }
}
