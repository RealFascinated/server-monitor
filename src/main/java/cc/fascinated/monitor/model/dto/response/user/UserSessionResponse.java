package cc.fascinated.monitor.model.dto.response.user;

import java.time.Instant;

public record UserSessionResponse(
        long id,
        Instant createdAt,
        Instant expiresAt,
        boolean current,
        String deviceLabel,
        String locationLabel
) {
}
