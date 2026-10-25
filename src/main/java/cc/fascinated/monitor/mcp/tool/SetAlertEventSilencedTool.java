package cc.fascinated.monitor.mcp.tool;

import cc.fascinated.monitor.alert.AlertHistoryService;
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
public class SetAlertEventSilencedTool {
    private final McpAuth auth;
    private final AlertHistoryService historyService;

    public SetAlertEventSilencedTool(McpAuth auth, AlertHistoryService historyService) {
        this.auth = auth;
        this.historyService = historyService;
    }

    @McpTool(name = "set_alert_event_silenced", description = """
            Silences or unsilences a firing alert event, i.e. acknowledges it. A silenced alert sends no \
            notifications (nor one when it resolves) and is not counted in firingCount, until its value gets worse \
            than it was when silenced; then it is unsilenced and notifies again. It stays open until it recovers. \
            Only works on events that are still firing (state=firing in list_alert_events); unsilencing an event \
            that is not silenced does nothing.""")
    public String setAlertEventSilenced(
            McpTransportContext context,
            @McpToolParam(description = "Event id from list_alert_events") long eventId,
            @McpToolParam(description = "true to silence, false to unsilence") boolean silenced
    ) {
        UserRow user = this.auth.require(context, ApiTokenScope.ALERTS_WRITE);
        if (silenced) {
            this.historyService.silence(user, eventId);
            return McpJson.write(new McpAck("Alert event %d silenced".formatted(eventId)));
        }
        this.historyService.unsilence(user, eventId);
        return McpJson.write(new McpAck("Alert event %d unsilenced".formatted(eventId)));
    }
}
