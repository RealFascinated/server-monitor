package cc.fascinated.monitor.model.dto.response.auth;

import java.time.Instant;

public record AuthTokenResponse(
        String token,
        Instant expiresAt
) {}
