package cc.fascinated.monitor.model.dto.response.server;

import cc.fascinated.monitor.model.persistance.IncidentRow;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public record IncidentResponse(
        long id,
        long serverId,
        Instant startedAt,
        @Nullable Instant resolvedAt
) {
    public static IncidentResponse from(IncidentRow row) {
        return new IncidentResponse(
                row.getId(),
                row.getServerId(),
                row.getStartedAt(),
                row.getResolvedAt()
        );
    }
}
