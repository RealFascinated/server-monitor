package cc.fascinated.monitor.model.dto.request.server;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ServerMemberInviteRequest(
        @NotBlank(message = "Email must not be empty")
        @Email(message = "Email must be valid")
        String email
) {}
