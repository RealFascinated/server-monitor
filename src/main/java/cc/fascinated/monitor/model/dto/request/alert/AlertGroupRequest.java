package cc.fascinated.monitor.model.dto.request.alert;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record AlertGroupRequest(
        @NotBlank(message = "Name must not be empty")
        @Size(max = 64, message = "Name must be at most 64 characters")
        String name,
        @Nullable Boolean autoAddServers,
        /** One of the caller's destinations; every alert in the group notifies it. */
        @Nullable Long destinationId,
        /** Seeds the group with the same alerts a new account gets. Only used when creating. */
        @Nullable Boolean addDefaultRules,
        @Nullable List<Long> serverIds
) {}
