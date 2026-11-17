package cc.fascinated.monitor.model.dto.request.alert;

import jakarta.validation.constraints.Size;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Only the fields that are present change; {@code serverIds} and {@code destinationIds} replace the whole list. */
public record AlertGroupUpdateRequest(
        @Nullable
        @Size(min = 1, max = 64, message = "Name must be between 1 and 64 characters")
        String name,
        @Nullable Boolean autoAddServers,
        @Nullable Boolean enabled,
        /** Replaces the group's destinations; an empty list stops it notifying anyone. */
        @Nullable List<Long> destinationIds,
        @Nullable List<Long> serverIds
) {}
