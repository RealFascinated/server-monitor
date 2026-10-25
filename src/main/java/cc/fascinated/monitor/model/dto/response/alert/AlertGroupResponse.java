package cc.fascinated.monitor.model.dto.response.alert;

import cc.fascinated.monitor.model.persistance.AlertGroupRow;

import java.time.Instant;
import java.util.List;

public record AlertGroupResponse(
        long id,
        String name,
        boolean autoAddServers,
        boolean enabled,
        boolean defaultGroup,
        List<GroupServer> servers,
        long ruleCount,
        long firingCount,
        Instant createdAt
) {

    public record GroupServer(long id, String name) {}

    public static AlertGroupResponse from(AlertGroupRow row, List<GroupServer> servers, long ruleCount, long firingCount) {
        return new AlertGroupResponse(row.getId(), row.getName(), row.isAutoAddServers(), row.isEnabled(),
                row.isDefaultGroup(), servers, ruleCount, firingCount,
                row.getCreatedAt());
    }
}
