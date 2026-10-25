package cc.fascinated.monitor.mcp.tool;

import cc.fascinated.monitor.alert.AlertHistoryService;
import cc.fascinated.monitor.mcp.McpJson;
import cc.fascinated.monitor.mcp.auth.McpAuth;
import cc.fascinated.monitor.mcp.auth.RequiresScope;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.dto.response.alert.AlertEventResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.util.Pagination;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class ListAlertEventsTool {
    private static final int DEFAULT_COUNT = 20;

    private final McpAuth auth;
    private final AlertHistoryService historyService;

    public ListAlertEventsTool(McpAuth auth, AlertHistoryService historyService) {
        this.auth = auth;
        this.historyService = historyService;
    }

    @RequiresScope(ApiTokenScope.ALERTS_READ)
    @McpTool(name = "list_alert_events", description = """
            Alert history, newest first. Each event is one firing of one rule on one server and series (labels, \
            e.g. which disk), with the rule's metric, metricLabel, unit, operator and threshold, startedAt, \
            resolvedAt and resolveReason (RECOVERED, SERIES_GONE = the series stopped reporting, RULE_CHANGED = \
            the rule was edited or disabled, its group was disabled, or the server left the group), peakValue (the worst value, \
            in the rule's unit), lastNotifiedAt, notifyError (why the last notification failed), and \
            silencedAt/silencedValue when muted. Use state=firing for "what is firing right now" (silenced alerts \
            included). Filter by rule, group or server to see how an alert has behaved. Returns a page: items, \
            totalItems, itemsPerPage, totalPages.""")
    public String listAlertEvents(
            McpTransportContext context,
            @McpToolParam(description = "firing (not resolved yet) or resolved; omit for both", required = false) String state,
            @McpToolParam(description = "Only events of this rule, id from list_alert_rules", required = false) Long ruleId,
            @McpToolParam(description = "Only events of rules in this group, id from list_alert_groups", required = false) Long groupId,
            @McpToolParam(description = "Only events on this server, id from list_servers", required = false) Long serverId,
            @McpToolParam(description = "Page number, starting at 1 (default 1)", required = false) Integer page,
            @McpToolParam(description = "Items per page, 1-50 (default 20)", required = false) Integer count
    ) {
        UserRow user = this.auth.require(context, ApiTokenScope.ALERTS_READ);
        return McpJson.write(this.historyService.list(user, page == null ? 1 : page, count == null ? DEFAULT_COUNT : count,
                ruleId, groupId, serverId, state));
    }
}
