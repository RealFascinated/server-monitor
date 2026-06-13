package cc.fascinated.monitor.model.dto.request.server;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderServerFoldersRequest(@NotEmpty List<Long> folderIds) {}
