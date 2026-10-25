package cc.fascinated.monitor.mcp.tool;

import cc.fascinated.monitor.alert.AlertGroupService;
import cc.fascinated.monitor.exception.impl.BadRequestException;
import cc.fascinated.monitor.mcp.McpJson;
import cc.fascinated.monitor.mcp.auth.McpAuth;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.dto.request.alert.AlertGroupUpdateRequest;
import cc.fascinated.monitor.model.dto.response.alert.AlertGroupResponse;
import cc.fascinated.monitor.model.persistance.UserRow;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class SetAlertGroupServersTool {
    private final McpAuth auth;
    private final AlertGroupService groupService;

    public SetAlertGroupServersTool(McpAuth auth, AlertGroupService groupService) {
        this.auth = auth;
        this.groupService = groupService;
    }

    @McpTool(name = "set_alert_group_servers", description = """
            Adds and/or removes servers from an alert group. Servers in the group are checked against all of its \
            rules; removing a server from a group is how to exclude it from those alerts, and closes its open \
            alerts there. Adding a server that is already in the group, or removing one that is not, does nothing. \
            A removed server is not added back by autoAddServers; that only applies to newly added servers. \
            Returns the updated group as list_alert_groups shows it.""")
    public String setAlertGroupServers(
            McpTransportContext context,
            @McpToolParam(description = "Group id from list_alert_groups") long groupId,
            @McpToolParam(description = "Server ids to add, from list_servers", required = false) List<Long> add,
            @McpToolParam(description = "Server ids to remove", required = false) List<Long> remove
    ) {
        UserRow user = this.auth.require(context, ApiTokenScope.ALERTS_WRITE);
        if ((add == null || add.isEmpty()) && (remove == null || remove.isEmpty())) {
            throw new BadRequestException("Pass at least one server id in add or remove");
        }
        if (add != null && remove != null && add.stream().anyMatch(remove::contains)) {
            throw new BadRequestException("A server cannot be in both add and remove");
        }
        Set<Long> serverIds = new LinkedHashSet<>();
        this.groupService.get(user, groupId).servers().forEach(server -> serverIds.add(server.id()));
        if (add != null) {
            serverIds.addAll(add);
        }
        if (remove != null) {
            serverIds.removeAll(remove);
        }
        return McpJson.write(this.groupService.update(user, groupId,
                new AlertGroupUpdateRequest(null, null, null, null, null, List.copyOf(serverIds))));
    }
}
