package cc.fascinated.monitor.mcp.tool;

import cc.fascinated.monitor.alert.AlertRuleService;
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
public class DeleteAlertRuleTool {
    private final McpAuth auth;
    private final AlertRuleService ruleService;

    public DeleteAlertRuleTool(McpAuth auth, AlertRuleService ruleService) {
        this.auth = auth;
        this.ruleService = ruleService;
    }

    @McpTool(name = "delete_alert_rule", description = """
            Permanently deletes an alert rule together with its whole alert history (its events in \
            list_alert_events), for every server in its group. This cannot be undone. To stop a rule without losing \
            it or its history, use update_alert_rule with enabled=false instead.""")
    public String deleteAlertRule(
            McpTransportContext context,
            @McpToolParam(description = "Rule id from list_alert_rules") long ruleId
    ) {
        UserRow user = this.auth.require(context, ApiTokenScope.ALERTS_WRITE);
        this.ruleService.delete(user, ruleId);
        return McpJson.write(new McpAck("Alert rule %d deleted".formatted(ruleId)));
    }
}
