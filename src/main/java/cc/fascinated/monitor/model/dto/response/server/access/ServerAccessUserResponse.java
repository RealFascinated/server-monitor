package cc.fascinated.monitor.model.dto.response.server.access;

import cc.fascinated.monitor.model.persistance.UserRow;

public record ServerAccessUserResponse(
        long id,
        String email
) {
    public static ServerAccessUserResponse from(UserRow user) {
        return new ServerAccessUserResponse(user.getId(), user.getEmail());
    }
}
