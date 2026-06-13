package cc.fascinated.monitor.model.dto.request.server;

import jakarta.validation.constraints.Size;
import org.jetbrains.annotations.Nullable;

public record UpdateServerFolderRequest(
        @Nullable
        @Size(max = 20, message = "Folder name must be at most 20 characters")
        String folderName
) {}
