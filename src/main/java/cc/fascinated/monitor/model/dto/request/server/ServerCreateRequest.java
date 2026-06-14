package cc.fascinated.monitor.model.dto.request.server;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jetbrains.annotations.Nullable;

public record ServerCreateRequest(
        @NotBlank(message = "Name must not be empty")
        @Size(max = 64, message = "Name must be at most 64 characters")
        String name,
        @Nullable
        @Size(max = 20, message = "Folder name must be at most 20 characters")
        String folderName
) {}
