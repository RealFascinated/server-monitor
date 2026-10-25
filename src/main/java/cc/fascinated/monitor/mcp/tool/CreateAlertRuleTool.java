package cc.fascinated.monitor.mcp.tool;

import cc.fascinated.monitor.alert.AlertRuleService;
import cc.fascinated.monitor.mcp.McpJson;
import cc.fascinated.monitor.mcp.McpMetricCatalogService;
import cc.fascinated.monitor.mcp.auth.McpAuth;
import cc.fascinated.monitor.model.domain.alert.AlertOperator;
import cc.fascinated.monitor.model.domain.alert.AlertSeverity;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.dto.request.alert.AlertRuleRequest;
import cc.fascinated.monitor.model.dto.response.alert.AlertRuleResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CreateAlertRuleTool {
    private final McpAuth auth;
    private final AlertRuleService ruleService;

    public CreateAlertRuleTool(McpAuth auth, AlertRuleService ruleService) {
        this.auth = auth;
        this.ruleService = ruleService;
    }

    @McpTool(name = "create_alert_rule", description = """
            Creates an alert rule in an alert group. Rules belong to a group, never to a server: every server in the \
            group is checked against the rule, each series separately (each disk, interface, ...), and the group's \
            destination is notified when the group is enabled and has one (events are recorded either way). Run \
            preview_alert_rule first to check the threshold against real history. The metric must have \
            alertable=true in list_metrics; its unit (PERCENT is 0-100) is the unit of threshold and \
            recoveryThreshold, and suggestedOperator/suggestedThreshold are sensible starting points. For "server \
            offline" use monitor_server_up with LESS_THAN and a threshold above 0 and at most 1 (window and \
            recovery are ignored for it). Returns the created rule as list_alert_rules shows it.""")
    public String createAlertRule(
            McpTransportContext context,
            @McpToolParam(description = "Group the rule belongs to, id from list_alert_groups") long groupId,
            @McpToolParam(description = "Metric id from list_metrics with alertable=true") String metric,
            @McpToolParam(description = "MORE_THAN fires when the value goes above the threshold, LESS_THAN when it goes below") AlertOperator operator,
            @McpToolParam(description = "Threshold in the metric's unit") double threshold,
            @McpToolParam(description = "WARNING or CRITICAL; only changes the notification's colour and label") AlertSeverity severity,
            @McpToolParam(description = "Rule name, at most 100 characters (default: none, shown as the metric's displayName)", required = false) String name,
            @McpToolParam(description = "Only series with these label values, e.g. {\"disk\":\"/\"}; at most 5, keys from labelKeys and values from series in list_metrics with serverId (default: every series)", required = false) Map<String, String> labels,
            @McpToolParam(description = "Value the alert must get back past to resolve, so it does not flap: for MORE_THAN at or below threshold, for LESS_THAN at or above it (default: the threshold)", required = false) Double recoveryThreshold,
            @McpToolParam(description = "Compare the rolling average over this many seconds, i.e. \"sustained for\": 0 = each sample, otherwise 10-86400; the rule cannot fire until a full window of data exists (default 0)", required = false) Integer windowSeconds,
            @McpToolParam(description = "Re-notify every this many seconds while still firing, 60-2592000 (30 days); omit or 0 to notify only when it fires and resolves", required = false) Integer repeatIntervalSeconds,
            @McpToolParam(description = "Start enabled (default true)", required = false) Boolean enabled
    ) {
        UserRow user = this.auth.require(context, ApiTokenScope.ALERTS_WRITE);
        McpMetricCatalogService.requireAlertable(metric);
        Integer repeat = repeatIntervalSeconds == null || repeatIntervalSeconds == 0 ? null : repeatIntervalSeconds;
        return McpJson.write(this.ruleService.create(user, new AlertRuleRequest(groupId, name, metric, labels, operator, threshold,
                recoveryThreshold, windowSeconds, severity, repeat, enabled)));
    }
}
