package cc.fascinated.monitor.model.dto.response.user;

import cc.fascinated.monitor.model.persistance.UserSessionRow;

import java.time.Instant;

public record UserSessionResponse(
        long id,
        Instant createdAt,
        Instant expiresAt,
        boolean current
) {
    public static UserSessionResponse from(UserSessionRow session, boolean current) {
        return new UserSessionResponse(session.getId(), session.getCreatedAt(), session.getExpiresAt(), current);
    }
}
