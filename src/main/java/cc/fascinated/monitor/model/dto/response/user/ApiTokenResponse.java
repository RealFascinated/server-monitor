package cc.fascinated.monitor.model.dto.response.user;

import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.persistance.ApiTokenRow;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

public record ApiTokenResponse(
        long id,
        String name,
        String prefix,
        List<String> scopes,
        Instant createdAt,
        @Nullable Instant expiresAt,
        @Nullable Instant lastUsedAt
) {

    public static ApiTokenResponse from(ApiTokenRow row, List<ApiTokenScope> scopes) {
        return new ApiTokenResponse(row.getId(), row.getName(), row.getTokenPrefix(),
                scopes.stream().map(ApiTokenScope::id).toList(), row.getCreatedAt(), row.getExpiresAt(),
                row.getLastUsedAt());
    }
}
