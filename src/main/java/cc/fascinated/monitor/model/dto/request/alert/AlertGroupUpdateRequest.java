package cc.fascinated.monitor.model.dto.request.alert;

import jakarta.validation.constraints.Size;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Only the fields that are present change; {@code serverIds} replaces the group's whole server list. */
public record AlertGroupUpdateRequest(
        @Nullable
        @Size(min = 1, max = 64, message = "Name must be between 1 and 64 characters")
        String name,
        @Nullable Boolean autoAddServers,
        @Nullable Boolean enabled,
        @Nullable Long destinationId,
        /** Stops the group notifying anyone; {@code destinationId} cannot express "none" because null means unchanged. */
        @Nullable Boolean clearDestination,
        @Nullable List<Long> serverIds
) {}
