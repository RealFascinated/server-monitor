package cc.fascinated.monitor.mcp.tool;

import cc.fascinated.monitor.alert.AlertGroupService;
import cc.fascinated.monitor.mcp.McpJson;
import cc.fascinated.monitor.mcp.auth.McpAuth;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.dto.response.alert.AlertGroupResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListAlertGroupsTool {
    private final McpAuth auth;
    private final AlertGroupService groupService;

    public ListAlertGroupsTool(McpAuth auth, AlertGroupService groupService) {
        this.auth = auth;
        this.groupService = groupService;
    }

    @McpTool(name = "list_alert_groups", description = """
            Lists alert groups. Alert groups are account-wide and rules belong to a group, never to a server: a \
            server is covered by the rules of every group it is a member of, and a group notifies its one \
            destination. Each group has servers (its members, id and name), destinationId/destinationName (absent = \
            notifies nobody), enabled (a disabled group evaluates none of its rules), autoAddServers (new servers \
            join it automatically), defaultGroup (the account's built-in group, which cannot be deleted), \
            ruleCount, and firingCount (its alerts open right now, silenced ones excluded). To exclude a server \
            from a group's alerts, remove it with set_alert_group_servers; list its rules with list_alert_rules.""")
    public String listAlertGroups(
            McpTransportContext context,
            @McpToolParam(description = "Return only this group (still as a one-element list)", required = false) Long groupId
    ) {
        UserRow user = this.auth.require(context, ApiTokenScope.ALERTS_READ);
        if (groupId != null) {
            return McpJson.write(List.of(this.groupService.get(user, groupId)));
        }
        return McpJson.write(this.groupService.list(user));
    }
}
