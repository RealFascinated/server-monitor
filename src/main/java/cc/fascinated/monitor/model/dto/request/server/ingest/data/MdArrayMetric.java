package cc.fascinated.monitor.model.dto.request.server.ingest.data;

import jakarta.validation.constraints.NotBlank;
import org.jetbrains.annotations.Nullable;

public record MdArrayMetric(
        @NotBlank(message = "Array name must not be empty")
        String name,
        @NotBlank(message = "Array level must not be empty")
        String level,
        Long disksTotal,
        Long disksActive,
        Long disksFailed,
        Boolean degraded,
        @Nullable String syncAction,
        @Nullable Double syncPercent,
        @Nullable Long syncSpeedKBps,
        @Nullable Double syncFinishMinutes
) {}
