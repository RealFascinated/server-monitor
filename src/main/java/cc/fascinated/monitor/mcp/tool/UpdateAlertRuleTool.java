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
public class UpdateAlertRuleTool {
    private final McpAuth auth;
    private final AlertRuleService ruleService;

    public UpdateAlertRuleTool(McpAuth auth, AlertRuleService ruleService) {
        this.auth = auth;
        this.ruleService = ruleService;
    }

    @McpTool(name = "update_alert_rule", description = """
            Changes an alert rule. Pass ruleId plus only the fields to change; the rest keep their current values \
            (see list_alert_rules), and the same limits as create_alert_rule apply. This is also how to disable or \
            re-enable a rule (enabled). A rule cannot move to another group: create a new one there and delete \
            this one. Changing metric, labels, operator, threshold, recoveryThreshold or windowSeconds, or \
            disabling the rule, resolves its open alerts; they fire again on the next evaluation if still true. \
            Use preview_alert_rule to check a new threshold first. Returns the updated rule.""")
    public String updateAlertRule(
            McpTransportContext context,
            @McpToolParam(description = "Rule id from list_alert_rules") long ruleId,
            @McpToolParam(description = "New rule name, at most 100 characters; \"\" removes it", required = false) String name,
            @McpToolParam(description = "New metric id from list_metrics with alertable=true; also pass labels ({} for none) if the current filter does not fit the new metric", required = false) String metric,
            @McpToolParam(description = "New label filter, replacing the old one entirely; {} matches every series", required = false) Map<String, String> labels,
            @McpToolParam(description = "MORE_THAN or LESS_THAN; also pass a recoveryThreshold on the new side if the rule has one", required = false) AlertOperator operator,
            @McpToolParam(description = "New threshold in the metric's unit", required = false) Double threshold,
            @McpToolParam(description = "New recovery threshold; set it equal to threshold to remove the hysteresis", required = false) Double recoveryThreshold,
            @McpToolParam(description = "New averaging window in seconds: 0 turns averaging off, otherwise 10-86400", required = false) Integer windowSeconds,
            @McpToolParam(description = "WARNING or CRITICAL", required = false) AlertSeverity severity,
            @McpToolParam(description = "New repeat interval in seconds, 60-2592000; 0 stops repeat notifications", required = false) Integer repeatIntervalSeconds,
            @McpToolParam(description = "Enable or disable the rule", required = false) Boolean enabled
    ) {
        UserRow user = this.auth.require(context, ApiTokenScope.ALERTS_WRITE);
        if (metric != null) {
            McpMetricCatalogService.requireAlertable(metric);
        }
        AlertRuleResponse current = this.ruleService.get(user, ruleId);
        Integer repeat = repeatIntervalSeconds == null ? current.repeatIntervalSeconds()
                : repeatIntervalSeconds == 0 ? null : repeatIntervalSeconds;
        return McpJson.write(this.ruleService.update(user, ruleId, new AlertRuleRequest(
                current.groupId(),
                name != null ? name : current.name(),
                metric != null ? metric : current.metric(),
                labels != null ? labels : current.labelFilter(),
                operator != null ? operator : current.operator(),
                threshold != null ? threshold : current.threshold(),
                recoveryThreshold != null ? recoveryThreshold : current.recoveryThreshold(),
                windowSeconds != null ? windowSeconds : current.windowSeconds(),
                severity != null ? severity : current.severity(),
                repeat,
                enabled != null ? enabled : current.enabled())));
    }
}
