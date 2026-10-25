package cc.fascinated.monitor.model.domain.user;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/** What an API token may do. {@link #ALERTS_WRITE} also grants {@link #ALERTS_READ}. */
@Getter
@Accessors(fluent = true)
public enum ApiTokenScope {
    SERVERS_READ("servers:read", "List servers with their status, hardware, current usage and outages"),
    METRICS_READ("metrics:read", "Browse the metric catalog and query metric history"),
    ALERTS_READ("alerts:read", "Read alert rules, groups, destinations and history, and preview rules against metric history"),
    ALERTS_WRITE("alerts:write", "Create, change, delete and silence alert rules, change which servers a group covers and send test notifications. Includes alerts:read");

    private final String id;
    private final String description;

    ApiTokenScope(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public static Optional<ApiTokenScope> fromId(String id) {
        return Arrays.stream(values()).filter(scope -> scope.id.equals(id)).findFirst();
    }

    /** Whether holding {@code granted} lets the caller use something that requires this scope. */
    public boolean isGrantedBy(Set<ApiTokenScope> granted) {
        return granted.contains(this) || (this == ALERTS_READ && granted.contains(ALERTS_WRITE));
    }
}
