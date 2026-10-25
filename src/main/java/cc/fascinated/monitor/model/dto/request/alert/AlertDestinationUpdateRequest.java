package cc.fascinated.monitor.model.dto.request.alert;

import jakarta.validation.constraints.Size;
import org.jetbrains.annotations.Nullable;

/** Fields left null are kept as they are, so a rename never needs the secret URL again. */
public record AlertDestinationUpdateRequest(
        @Nullable
        @Size(min = 1, max = 64, message = "Name must be between 1 and 64 characters")
        String name,
        @Nullable
        @Size(min = 1, max = 512, message = "Webhook URL must be between 1 and 512 characters")
        String config
) {}
