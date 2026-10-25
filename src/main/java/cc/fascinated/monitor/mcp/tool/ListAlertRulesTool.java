package cc.fascinated.monitor.mcp.tool;

import cc.fascinated.monitor.alert.AlertRuleService;
import cc.fascinated.monitor.mcp.McpJson;
import cc.fascinated.monitor.mcp.auth.McpAuth;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.dto.response.alert.AlertRuleResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListAlertRulesTool {
    private final McpAuth auth;
    private final AlertRuleService ruleService;

    public ListAlertRulesTool(McpAuth auth, AlertRuleService ruleService) {
        this.auth = auth;
        this.ruleService = ruleService;
    }

    @McpTool(name = "list_alert_rules", description = """
            Lists the user's alert rules, newest first. Every rule belongs to one alert group and applies to every \
            server in that group. Each rule has groupId/groupName, name (absent when unnamed), metric with \
            metricLabel and unit (thresholds and values are in this unit), labelFilter (empty = every series), \
            operator (MORE_THAN or LESS_THAN), threshold, recoveryThreshold (absent = resolves as soon as the \
            threshold is no longer crossed), windowSeconds (rolling average, 0 = raw value), severity (WARNING or \
            CRITICAL), repeatIntervalSeconds (absent = notify only on fire and resolve), enabled, and its state now: \
            firingCount and silencedCount are the alerts open at this moment, lastFiredAt the newest firing ever. \
            Use list_alert_events for the history. Pass ruleId for one rule.""")
    public String listAlertRules(
            McpTransportContext context,
            @McpToolParam(description = "Only rules in this group, id from list_alert_groups", required = false) Long groupId,
            @McpToolParam(description = "Return only this rule (still as a one-element list); groupId is then ignored", required = false) Long ruleId
    ) {
        UserRow user = this.auth.require(context, ApiTokenScope.ALERTS_READ);
        if (ruleId != null) {
            return McpJson.write(List.of(this.ruleService.get(user, ruleId)));
        }
        return McpJson.write(this.ruleService.list(user, groupId));
    }
}
