package cc.fascinated.monitor.model.dto.response.auth;

import cc.fascinated.monitor.model.domain.user.UserRole;
import cc.fascinated.monitor.model.persistance.UserRow;

import java.time.Instant;

public record UserResponse(
        long id,
        String email,
        UserRole role,
        Instant createdAt
) {
    public static UserResponse from(UserRow user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
