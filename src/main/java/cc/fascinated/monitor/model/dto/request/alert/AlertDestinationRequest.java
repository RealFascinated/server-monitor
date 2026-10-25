package cc.fascinated.monitor.model.dto.request.alert;

import cc.fascinated.monitor.model.domain.alert.AlertDestination;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlertDestinationRequest(
        @NotBlank(message = "Name must not be empty")
        @Size(max = 64, message = "Name must be at most 64 characters")
        String name,
        @NotNull(message = "Type is required")
        AlertDestination type,
        @NotBlank(message = "Webhook URL must not be empty")
        @Size(max = 512, message = "Webhook URL must be at most 512 characters")
        String config
) {}
