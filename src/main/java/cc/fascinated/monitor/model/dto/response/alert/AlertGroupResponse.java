package cc.fascinated.monitor.model.dto.response.alert;

import cc.fascinated.monitor.model.persistance.AlertGroupRow;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

public record AlertGroupResponse(
        long id,
        String name,
        boolean autoAddServers,
        boolean enabled,
        boolean defaultGroup,
        @Nullable Long destinationId,
        @Nullable String destinationName,
        List<GroupServer> servers,
        long ruleCount,
        long firingCount,
        Instant createdAt
) {

    public record GroupServer(long id, String name) {}

    public static AlertGroupResponse from(AlertGroupRow row, @Nullable String destinationName, List<GroupServer> servers,
                                          long ruleCount, long firingCount) {
        return new AlertGroupResponse(row.getId(), row.getName(), row.isAutoAddServers(), row.isEnabled(),
                row.isDefaultGroup(), row.getDestinationId(), destinationName, servers, ruleCount, firingCount,
                row.getCreatedAt());
    }
}
