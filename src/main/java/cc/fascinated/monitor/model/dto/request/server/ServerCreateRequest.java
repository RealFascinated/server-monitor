package cc.fascinated.monitor.model.dto.request.server;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServerCreateRequest(
        @NotBlank(message = "Name must not be empty")
        @Size(max = 20, message = "Name must be at most 20 characters")
        String name
) {}
