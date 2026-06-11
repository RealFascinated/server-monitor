package cc.fascinated.monitor.model.dto.request.server;

import jakarta.validation.constraints.NotBlank;

public record ServerInviteAcceptRequest(
        @NotBlank(message = "Token must not be empty")
        String token
) {}
