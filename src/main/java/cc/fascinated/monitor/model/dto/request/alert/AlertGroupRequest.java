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
        @Nullable List<Long> serverIds
) {}
