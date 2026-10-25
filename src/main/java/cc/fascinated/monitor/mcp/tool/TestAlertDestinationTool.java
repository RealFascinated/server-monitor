package cc.fascinated.monitor.mcp.tool;

import cc.fascinated.monitor.alert.AlertDestinationService;
import cc.fascinated.monitor.mcp.McpJson;
import cc.fascinated.monitor.mcp.auth.McpAuth;
import cc.fascinated.monitor.mcp.dto.McpAck;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.persistance.UserRow;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class TestAlertDestinationTool {
    private final McpAuth auth;
    private final AlertDestinationService destinationService;

    public TestAlertDestinationTool(McpAuth auth, AlertDestinationService destinationService) {
        this.auth = auth;
        this.destinationService = destinationService;
    }

    @McpTool(name = "test_alert_destination", description = """
            Sends a sample alert to an alert destination (e.g. posts it to the Discord channel) to check that \
            delivery works; the error says why when it does not. It notifies real people, so only call it when the \
            user asked to test delivery.""")
    public String testAlertDestination(
            McpTransportContext context,
            @McpToolParam(description = "Destination id from list_alert_destinations") long destinationId
    ) {
        UserRow user = this.auth.require(context, ApiTokenScope.ALERTS_WRITE);
        this.destinationService.test(user, destinationId);
        return McpJson.write(new McpAck("Test message sent to destination %d".formatted(destinationId)));
    }
}
